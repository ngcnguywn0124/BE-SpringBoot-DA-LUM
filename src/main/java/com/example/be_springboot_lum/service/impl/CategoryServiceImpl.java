package com.example.be_springboot_lum.service.impl;

import com.example.be_springboot_lum.dto.request.CategoryRequest;
import com.example.be_springboot_lum.dto.response.CategoryResponse;
import com.example.be_springboot_lum.exception.AppException;
import com.example.be_springboot_lum.exception.ErrorCode;
import com.example.be_springboot_lum.model.Category;
import com.example.be_springboot_lum.repository.CategoryRepository;
import com.example.be_springboot_lum.service.CategoryService;
import com.example.be_springboot_lum.service.CloudinaryService;
import com.example.be_springboot_lum.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CloudinaryService  cloudinaryService;

    @Value("${cloudinary.upload-folder:lum}")
    private String baseFolder;

    private static final String CATEGORY_FOLDER_SUFFIX = "/categories";

    // ─── Queries ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoryTree() {
        return categoryRepository
                .findByParentIsNullOrderByDisplayOrderAscCategoryNameAsc()
                .stream()
                .map(c -> toResponse(c, true))   // true = kèm danh sách con
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories(String keyword) {
        List<Category> categories;
        if (StringUtils.hasText(keyword)) {
            categories = categoryRepository
                    .findByCategoryNameContainingIgnoreCaseOrderByCategoryNameAsc(keyword);
        } else {
            categories = categoryRepository
                    .findByParentIsNullOrderByDisplayOrderAscCategoryNameAsc();
        }
        return categories.stream()
                .map(c -> toResponse(c, false))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getChildCategories(UUID parentId) {
        // Kiểm tra cha tồn tại
        categoryRepository.findById(parentId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        return categoryRepository
                .findByParent_CategoryIdOrderByDisplayOrderAscCategoryNameAsc(parentId)
                .stream()
                .map(c -> toResponse(c, false))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        return toResponse(category, true);
    }

    // ─── Mutations ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request, MultipartFile image) {
        Category parent = resolveParent(request.getParentCategoryId());
        checkDuplicateName(request.getCategoryName(), parent, null);

        String slug = SlugUtils.toSlug(request.getCategoryName());

        Category category = Category.builder()
                .categoryName(request.getCategoryName())
                .slug(slug)
                .description(request.getDescription())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .parent(parent)
                .build();

        if (image != null && !image.isEmpty()) {
            var uploadResult = cloudinaryService.upload(image, baseFolder + CATEGORY_FOLDER_SUFFIX);
            category.setImageUrl(uploadResult.getUrl());
            category.setImageCloudId(uploadResult.getPublicId());
        }

        return toResponse(categoryRepository.save(category), true);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(UUID id, CategoryRequest request, MultipartFile image) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        Category parent = resolveParent(request.getParentCategoryId());

        // Kiểm tra circular reference
        if (parent != null) {
            validateNotCircular(id, parent);
        }

        // Kiểm tra tên trùng (trừ chính nó)
        checkDuplicateName(request.getCategoryName(), parent, id);

        category.setCategoryName(request.getCategoryName());
        category.setSlug(SlugUtils.toSlug(request.getCategoryName()));
        category.setDescription(request.getDescription());
        category.setParent(parent);
        if (request.getDisplayOrder() != null) {
            category.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getIsActive() != null) {
            category.setIsActive(request.getIsActive());
        }

        // Thay ảnh nếu có file mới
        if (image != null && !image.isEmpty()) {
            deleteOldImage(category.getImageCloudId());
            var uploadResult = cloudinaryService.upload(image, baseFolder + CATEGORY_FOLDER_SUFFIX);
            category.setImageUrl(uploadResult.getUrl());
            category.setImageCloudId(uploadResult.getPublicId());
        }

        return toResponse(categoryRepository.save(category), true);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategoryImage(UUID id, MultipartFile image) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        deleteOldImage(category.getImageCloudId());

        var uploadResult = cloudinaryService.upload(image, baseFolder + CATEGORY_FOLDER_SUFFIX);
        category.setImageUrl(uploadResult.getUrl());
        category.setImageCloudId(uploadResult.getPublicId());

        return toResponse(categoryRepository.save(category), false);
    }

    @Override
    @Transactional
    public void deleteCategory(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        // Không xóa nếu còn danh mục con
        if (!category.getChildren().isEmpty()) {
            throw new AppException(ErrorCode.CATEGORY_HAS_CHILDREN);
        }

        // Không xóa nếu còn sản phẩm
        if (categoryRepository.countProductsByCategory(id) > 0) {
            throw new AppException(ErrorCode.CATEGORY_HAS_PRODUCTS);
        }

        // Xóa ảnh trên Cloudinary trước khi xóa entity
        deleteOldImage(category.getImageCloudId());

        categoryRepository.delete(category);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Category resolveParent(UUID parentId) {
        if (parentId == null) return null;
        return categoryRepository.findById(parentId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND,
                        "Không tìm thấy danh mục cha"));
    }

    /**
     * Kiểm tra tên danh mục đã tồn tại trong cùng cấp cha hay chưa.
     * @param excludeId ID của danh mục đang cập nhật (null khi tạo mới → dùng UUID rỗng để không bỏ qua gì)
     */
    private void checkDuplicateName(String name, Category parent, UUID excludeId) {
        UUID parentId = parent != null ? parent.getCategoryId() : null;
        UUID safeExcludeId = excludeId != null ? excludeId : UUID.fromString("00000000-0000-0000-0000-000000000000");

        boolean exists = categoryRepository.existsByNameAtSameLevelExcluding(name, parentId, safeExcludeId);
        if (exists) {
            throw new AppException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }
    }

    /** Ngăn gán một danh mục làm con của chính nó hoặc con của nó */
    private void validateNotCircular(UUID categoryId, Category newParent) {
        Category cursor = newParent;
        while (cursor != null) {
            if (cursor.getCategoryId().equals(categoryId)) {
                throw new AppException(ErrorCode.CATEGORY_CIRCULAR_REFERENCE);
            }
            cursor = cursor.getParent();
        }
    }

    private void deleteOldImage(String imageCloudId) {
        if (imageCloudId != null && !imageCloudId.isBlank()) {
            cloudinaryService.delete(imageCloudId);
        }
    }

    // ─── Mapper ───────────────────────────────────────────────────────────────

    private CategoryResponse toResponse(Category c, boolean includeChildren) {
        CategoryResponse.CategoryResponseBuilder builder = CategoryResponse.builder()
                .categoryId(c.getCategoryId())
                .categoryName(c.getCategoryName())
                .slug(c.getSlug())
                .description(c.getDescription())
                .imageUrl(c.getImageUrl())
                .imageCloudId(c.getImageCloudId())
                .displayOrder(c.getDisplayOrder())
                .isActive(c.getIsActive())
                .createdAt(c.getCreatedAt());

        if (c.getParent() != null) {
            builder.parentCategoryId(c.getParent().getCategoryId());
            builder.parentCategoryName(c.getParent().getCategoryName());
        }

        if (includeChildren && c.getChildren() != null && !c.getChildren().isEmpty()) {
            builder.children(c.getChildren().stream()
                    .map(child -> toResponse(child, true))
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }
}
