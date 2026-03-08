package com.example.be_springboot_lum.service.impl;

import com.example.be_springboot_lum.dto.request.ProductAttributeRequest;
import com.example.be_springboot_lum.dto.response.ProductAttributeResponse;
import com.example.be_springboot_lum.exception.AppException;
import com.example.be_springboot_lum.exception.ErrorCode;
import com.example.be_springboot_lum.model.AttributeType;
import com.example.be_springboot_lum.model.Category;
import com.example.be_springboot_lum.model.ProductAttribute;
import com.example.be_springboot_lum.repository.CategoryRepository;
import com.example.be_springboot_lum.repository.ProductAttributeRepository;
import com.example.be_springboot_lum.service.ProductAttributeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductAttributeServiceImpl implements ProductAttributeService {

    private final ProductAttributeRepository attributeRepository;
    private final CategoryRepository         categoryRepository;

    // ─── Queries ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ProductAttributeResponse> getAttributesByCategory(UUID categoryId) {
        // Kiểm tra danh mục tồn tại
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        return attributeRepository
                .findByCategory_CategoryIdOrderByDisplayOrderAscAttributeNameAsc(categoryId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductAttributeResponse getAttributeById(UUID id) {
        return toResponse(findOrThrow(id));
    }

    // ─── Mutations ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ProductAttributeResponse createAttribute(ProductAttributeRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        // Kiểm tra tên trùng trong cùng danh mục
        if (attributeRepository.existsByAttributeNameIgnoreCaseAndCategory_CategoryId(
                request.getAttributeName(), request.getCategoryId())) {
            throw new AppException(ErrorCode.PRODUCT_ATTRIBUTE_ALREADY_EXISTS);
        }

        // Validate options khi type = SELECT
        validateSelectOptions(request);

        ProductAttribute attribute = ProductAttribute.builder()
                .category(category)
                .attributeName(request.getAttributeName())
                .attributeType(request.getAttributeType())
                .isRequired(request.getIsRequired() != null ? request.getIsRequired() : false)
                .options(request.getAttributeType() == AttributeType.SELECT ? request.getOptions() : null)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .build();

        return toResponse(attributeRepository.save(attribute));
    }

    @Override
    @Transactional
    public ProductAttributeResponse updateAttribute(UUID id, ProductAttributeRequest request) {
        ProductAttribute attribute = findOrThrow(id);

        // Danh mục có thể thay đổi
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        // Kiểm tra tên trùng (loại trừ chính nó)
        if (attributeRepository.existsByAttributeNameIgnoreCaseAndCategory_CategoryIdAndAttributeIdNot(
                request.getAttributeName(), request.getCategoryId(), id)) {
            throw new AppException(ErrorCode.PRODUCT_ATTRIBUTE_ALREADY_EXISTS);
        }

        validateSelectOptions(request);

        attribute.setCategory(category);
        attribute.setAttributeName(request.getAttributeName());
        attribute.setAttributeType(request.getAttributeType());
        if (request.getIsRequired() != null) {
            attribute.setIsRequired(request.getIsRequired());
        }
        attribute.setOptions(request.getAttributeType() == AttributeType.SELECT ? request.getOptions() : null);
        if (request.getDisplayOrder() != null) {
            attribute.setDisplayOrder(request.getDisplayOrder());
        }

        return toResponse(attributeRepository.save(attribute));
    }

    @Override
    @Transactional
    public void deleteAttribute(UUID id) {
        if (!attributeRepository.existsById(id)) {
            throw new AppException(ErrorCode.PRODUCT_ATTRIBUTE_NOT_FOUND);
        }
        attributeRepository.deleteById(id);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private ProductAttribute findOrThrow(UUID id) {
        return attributeRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_ATTRIBUTE_NOT_FOUND));
    }

    /**
     * Khi kiểu là SELECT, danh sách options phải có ít nhất 2 phần tử.
     */
    private void validateSelectOptions(ProductAttributeRequest request) {
        if (request.getAttributeType() == AttributeType.SELECT) {
            if (request.getOptions() == null || request.getOptions().size() < 2) {
                throw new AppException(ErrorCode.PRODUCT_ATTRIBUTE_SELECT_OPTIONS_REQUIRED);
            }
        }
    }

    private ProductAttributeResponse toResponse(ProductAttribute a) {
        return ProductAttributeResponse.builder()
                .attributeId(a.getAttributeId())
                .categoryId(a.getCategory().getCategoryId())
                .categoryName(a.getCategory().getCategoryName())
                .attributeName(a.getAttributeName())
                .attributeType(a.getAttributeType())
                .isRequired(a.getIsRequired())
                .options(a.getOptions())
                .displayOrder(a.getDisplayOrder())
                .build();
    }
}
