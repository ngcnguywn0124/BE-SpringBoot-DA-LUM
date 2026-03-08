package com.example.be_springboot_lum.service;

import com.example.be_springboot_lum.dto.request.ProductAttributeRequest;
import com.example.be_springboot_lum.dto.response.ProductAttributeResponse;

import java.util.List;
import java.util.UUID;

/**
 * Quản lý định nghĩa thuộc tính động của sản phẩm theo danh mục.
 * Admin cấu hình tập thuộc tính (vd: RAM, Màu sắc, Trạng thái...) cho từng danh mục.
 */
public interface ProductAttributeService {

    /** Lấy danh sách thuộc tính của một danh mục */
    List<ProductAttributeResponse> getAttributesByCategory(UUID categoryId);

    /** Chi tiết một thuộc tính */
    ProductAttributeResponse getAttributeById(UUID id);

    /** Tạo thuộc tính mới cho danh mục */
    ProductAttributeResponse createAttribute(ProductAttributeRequest request);

    /** Cập nhật thuộc tính */
    ProductAttributeResponse updateAttribute(UUID id, ProductAttributeRequest request);

    /** Xóa thuộc tính */
    void deleteAttribute(UUID id);
}
