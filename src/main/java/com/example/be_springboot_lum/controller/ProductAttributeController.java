package com.example.be_springboot_lum.controller;

import com.example.be_springboot_lum.common.ApiResponse;
import com.example.be_springboot_lum.dto.request.ProductAttributeRequest;
import com.example.be_springboot_lum.dto.response.ProductAttributeResponse;
import com.example.be_springboot_lum.service.ProductAttributeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Quản lý Thuộc tính sản phẩm (Product Attributes)
 *
 * Thuộc tính là bộ trường động do Admin định nghĩa cho từng danh mục,
 * giúp người đăng điền thông tin chi tiết (vd: RAM, Màu sắc, Kích thước...).
 *
 * [Public]
 * GET  /api/v1/product-attributes?categoryId={uuid}  - Lấy danh sách thuộc tính theo danh mục
 * GET  /api/v1/product-attributes/{id}               - Chi tiết thuộc tính
 *
 * [ADMIN / SUPER_ADMIN]
 * POST   /api/v1/product-attributes        - Tạo thuộc tính mới
 * PUT    /api/v1/product-attributes/{id}   - Cập nhật thuộc tính
 * DELETE /api/v1/product-attributes/{id}   - Xóa thuộc tính
 */
@RestController
@RequestMapping("${api.prefix}/product-attributes")
@RequiredArgsConstructor
public class ProductAttributeController {

    private final ProductAttributeService attributeService;

    // ─── Public endpoints ────────────────────────────────────────────────────

    /**
     * Lấy toàn bộ thuộc tính của một danh mục, sắp theo display_order.
     * Dùng để frontend render form nhập thông tin chi tiết sản phẩm.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductAttributeResponse>>> getAttributesByCategory(
            @RequestParam UUID categoryId) {
        return ResponseEntity.ok(
                ApiResponse.success(attributeService.getAttributesByCategory(categoryId)));
    }

    /**
     * Chi tiết một thuộc tính.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductAttributeResponse>> getAttributeById(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(attributeService.getAttributeById(id)));
    }

    // ─── Admin endpoints ──────────────────────────────────────────────────────

    /**
     * Tạo thuộc tính mới cho một danh mục.
     * Nếu attributeType = "select", trường options phải có ít nhất 2 lựa chọn.
     *
     * <pre>
     * POST /api/v1/product-attributes
     * {
     *   "categoryId": "...",
     *   "attributeName": "RAM",
     *   "attributeType": "select",
     *   "isRequired": true,
     *   "options": ["4GB", "8GB", "16GB"],
     *   "displayOrder": 1
     * }
     * </pre>
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ProductAttributeResponse>> createAttribute(
            @Valid @RequestBody ProductAttributeRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.created(attributeService.createAttribute(request)));
    }

    /**
     * Cập nhật thông tin thuộc tính.
     * Có thể thay đổi danh mục, tên, kiểu, danh sách lựa chọn và thứ tự hiển thị.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ProductAttributeResponse>> updateAttribute(
            @PathVariable UUID id,
            @Valid @RequestBody ProductAttributeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật thuộc tính thành công",
                attributeService.updateAttribute(id, request)));
    }

    /**
     * Xóa thuộc tính.
     * Lưu ý: các giá trị thuộc tính (product_attribute_values) của sản phẩm
     * sẽ bị xóa theo (ON DELETE CASCADE trong DB).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteAttribute(@PathVariable UUID id) {
        attributeService.deleteAttribute(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa thuộc tính thành công", null));
    }
}
