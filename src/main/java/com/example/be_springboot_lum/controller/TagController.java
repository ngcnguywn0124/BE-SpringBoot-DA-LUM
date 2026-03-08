package com.example.be_springboot_lum.controller;

import com.example.be_springboot_lum.common.ApiResponse;
import com.example.be_springboot_lum.dto.request.TagRequest;
import com.example.be_springboot_lum.dto.response.TagResponse;
import com.example.be_springboot_lum.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Quản lý Tags / từ khóa sản phẩm
 *
 * [Public]
 * GET  /api/v1/tags           - Danh sách tags (tuỳ chọn ?keyword= để tìm kiếm)
 * GET  /api/v1/tags/{id}      - Chi tiết tag
 *
 * [ADMIN / SUPER_ADMIN]
 * POST   /api/v1/tags         - Tạo tag mới
 * PUT    /api/v1/tags/{id}    - Cập nhật tag
 * DELETE /api/v1/tags/{id}    - Xóa tag
 */
@RestController
@RequestMapping("${api.prefix}/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    // ─── Public endpoints ────────────────────────────────────────────────────

    /**
     * Lấy danh sách tag, sắp theo lượt dùng giảm dần.
     * Dùng ?keyword= để lọc nhanh (gợi ý khi người dùng nhập tag).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TagResponse>>> getAllTags(
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.success(tagService.getAllTags(keyword)));
    }

    /**
     * Chi tiết một tag.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TagResponse>> getTagById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(tagService.getTagById(id)));
    }

    // ─── Admin endpoints ──────────────────────────────────────────────────────

    /**
     * Tạo tag mới.
     * Slug được tự động sinh từ tagName (hỗ trợ tiếng Việt có dấu).
     *
     * <pre>
     * POST /api/v1/tags
     * { "tagName": "Laptop Gaming" }
     * </pre>
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TagResponse>> createTag(
            @Valid @RequestBody TagRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.created(tagService.createTag(request)));
    }

    /**
     * Cập nhật tên tag (slug được tự động cập nhật theo tên mới).
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TagResponse>> updateTag(
            @PathVariable UUID id,
            @Valid @RequestBody TagRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật tag thành công",
                tagService.updateTag(id, request)));
    }

    /**
     * Xóa tag.
     * Lưu ý: các liên kết sản phẩm – tag (product_tags) sẽ bị xóa theo (ON DELETE CASCADE).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteTag(@PathVariable UUID id) {
        tagService.deleteTag(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa tag thành công", null));
    }
}
