package com.example.be_springboot_lum.controller;

import com.example.be_springboot_lum.common.ApiResponse;
import com.example.be_springboot_lum.dto.request.CategoryRequest;
import com.example.be_springboot_lum.dto.response.CategoryResponse;
import com.example.be_springboot_lum.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Category Controller – Quản lý danh mục sản phẩm (có cha / con)
 *
 * [Public]
 * GET  /api/v1/categories/tree            - Cây danh mục đầy đủ (gốc + con)
 * GET  /api/v1/categories                 - Danh mục gốc hoặc tìm kiếm (?keyword=)
 * GET  /api/v1/categories/{id}            - Chi tiết danh mục kèm con trực tiếp
 * GET  /api/v1/categories/{id}/children   - Danh mục con trực tiếp
 *
 * [ADMIN / SUPER_ADMIN]
 * POST   /api/v1/categories               - Tạo danh mục mới (multipart: data + image)
 * PUT    /api/v1/categories/{id}          - Cập nhật danh mục (multipart: data + image)
 * PATCH  /api/v1/categories/{id}/image    - Chỉ thay ảnh
 * DELETE /api/v1/categories/{id}          - Xóa danh mục
 */
@RestController
@RequestMapping("${api.prefix}/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // ─── Public endpoints ─────────────────────────────────────────────────────

    /**
     * Trả về cây danh mục: gốc → con → cháu …
     * Dùng để render menu, bộ lọc danh mục phía frontend.
     */
    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategoryTree() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getCategoryTree()));
    }

    /**
     * Lấy danh mục gốc hoặc tìm kiếm toàn bộ theo từ khóa (không kèm con).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories(
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAllCategories(keyword)));
    }

    /**
     * Chi tiết một danh mục kèm danh mục con trực tiếp.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getCategoryById(id)));
    }

    /**
     * Danh mục con trực tiếp của một danh mục cha.
     */
    @GetMapping("/{id}/children")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getChildCategories(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getChildCategories(id)));
    }

    // ─── Admin endpoints ──────────────────────────────────────────────────────

    /**
     * Tạo danh mục mới.
     * Gửi multipart/form-data: part "data" (JSON) + part "image" (file, tùy chọn).
     *
     * <pre>
     * curl -X POST /api/v1/categories \
     *   -F "data={\"categoryName\":\"Laptop\",\"parentCategoryId\":null}" \
     *   -F "image=@/path/to/image.jpg"
     * </pre>
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestPart("data")               CategoryRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        return ResponseEntity.status(201)
                .body(ApiResponse.created(categoryService.createCategory(request, image)));
    }

    /**
     * Cập nhật danh mục.
     * Multipart/form-data: part "data" (JSON) + part "image" (file, tùy chọn).
     * Nếu không gửi "image" thì ảnh cũ được giữ nguyên.
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestPart("data")               CategoryRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật danh mục thành công",
                categoryService.updateCategory(id, request, image)));
    }

    /**
     * Chỉ cập nhật ảnh của danh mục (xóa ảnh cũ trên Cloudinary, upload ảnh mới).
     */
    @PatchMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategoryImage(
            @PathVariable UUID id,
            @RequestPart("image") MultipartFile image) {
        return ResponseEntity.ok(ApiResponse.success("Cập nhật ảnh thành công",
                categoryService.updateCategoryImage(id, image)));
    }

    /**
     * Xóa danh mục.
     * Điều kiện: không có danh mục con và không có sản phẩm đang dùng.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa danh mục thành công", null));
    }
}
