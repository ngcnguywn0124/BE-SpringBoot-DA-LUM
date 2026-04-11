package com.example.be_springboot_lum.service;

import com.example.be_springboot_lum.dto.request.BlogCategoryRequest;
import com.example.be_springboot_lum.dto.response.BlogCategoryResponse;
import com.example.be_springboot_lum.exception.AppException;
import com.example.be_springboot_lum.exception.ErrorCode;
import com.example.be_springboot_lum.model.BlogCategory;
import com.example.be_springboot_lum.repository.BlogCategoryRepository;
import com.example.be_springboot_lum.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BlogCategoryService {

    private final BlogCategoryRepository blogCategoryRepository;

    @Transactional(readOnly = true)
    public List<BlogCategoryResponse> getAllCategories(String keyword, Boolean isActive) {
        String safeKeyword = (keyword == null) ? "" : keyword;
        return blogCategoryRepository.searchCategories(safeKeyword, isActive)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public BlogCategoryResponse getCategoryById(UUID id) {
        BlogCategory category = blogCategoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_CATEGORY_NOT_FOUND));
        return mapToResponse(category);
    }

    @Transactional
    public BlogCategoryResponse createCategory(BlogCategoryRequest request) {
        String slug = SlugUtils.toSlug(request.getName());
        
        if (blogCategoryRepository.findByName(request.getName()).isPresent() || 
            blogCategoryRepository.findBySlug(slug).isPresent()) {
            throw new AppException(ErrorCode.BLOG_CATEGORY_ALREADY_EXISTS);
        }

        BlogCategory category = BlogCategory.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        return mapToResponse(blogCategoryRepository.save(category));
    }

    @Transactional
    public BlogCategoryResponse updateCategory(UUID id, BlogCategoryRequest request) {
        BlogCategory category = blogCategoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_CATEGORY_NOT_FOUND));

        if (request.getName() != null && !request.getName().equals(category.getName())) {
            String slug = SlugUtils.toSlug(request.getName());
            
            boolean duplicated = blogCategoryRepository.findAll().stream()
                    .anyMatch(c -> !c.getBlogCategoryId().equals(id) && 
                              (c.getName().equalsIgnoreCase(request.getName()) || c.getSlug().equals(slug)));
            
            if (duplicated) {
                throw new AppException(ErrorCode.BLOG_CATEGORY_ALREADY_EXISTS);
            }
            
            category.setName(request.getName());
            category.setSlug(slug);
        }

        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }

        if (request.getIsActive() != null) {
            category.setIsActive(request.getIsActive());
        }

        return mapToResponse(blogCategoryRepository.save(category));
    }

    @Transactional
    public void deleteCategory(UUID id) {
        if (!blogCategoryRepository.existsById(id)) {
            throw new AppException(ErrorCode.BLOG_CATEGORY_NOT_FOUND);
        }
        blogCategoryRepository.deleteById(id);
    }

    private BlogCategoryResponse mapToResponse(BlogCategory category) {
        return BlogCategoryResponse.builder()
                .blogCategoryId(category.getBlogCategoryId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .color(category.getColor())
                .icon(category.getIcon())
                .displayOrder(category.getDisplayOrder())
                .postCount(category.getPostCount())
                .isActive(category.getIsActive())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
