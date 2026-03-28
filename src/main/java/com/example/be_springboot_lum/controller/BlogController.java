package com.example.be_springboot_lum.controller;

import com.example.be_springboot_lum.common.ApiResponse;
import com.example.be_springboot_lum.dto.request.BlogStatusRequest;
import com.example.be_springboot_lum.dto.request.CreateBlogRequest;
import com.example.be_springboot_lum.dto.response.BlogResponse;
import com.example.be_springboot_lum.model.User;
import com.example.be_springboot_lum.service.BlogService;
import com.example.be_springboot_lum.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("${api.prefix}/blogs")
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;
    private final SecurityUtils securityUtils;

    // ═════════════════════════════════════════════════════════════════════════
    // PUBLIC
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Lấy danh sách bài viết đã duyệt.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<BlogResponse>>> getBlogs(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean isFeatured,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        Pageable pageable = buildPageable(page, size, sort);
        return ResponseEntity.ok(ApiResponse.success(blogService.getApprovedBlogs(category, query, isFeatured, pageable)));
    }

    /**
     * Chi tiết bài viết theo slug.
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<BlogResponse>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(blogService.getBlogBySlug(slug)));
    }

    /**
     * Chi tiết bài viết theo ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BlogResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(blogService.getBlogById(id)));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // AUTHENTICATED – Sinh viên
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Đăng bài viết mới.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<BlogResponse>> createBlog(
            @Valid @ModelAttribute CreateBlogRequest request) throws IOException {

        User author = securityUtils.getCurrentUser();
        return ResponseEntity.status(201)
                .body(ApiResponse.created(blogService.createBlog(request, author)));
    }

    /**
     * Xóa bài viết (chỉ tác giả hoặc admin).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteBlog(@PathVariable UUID id) {
        User actor = securityUtils.getCurrentUser();
        blogService.deleteBlog(id, actor);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa bài viết thành công", null));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ADMIN / MODERATOR
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Admin xem tất cả bài viết (theo trạng thái).
     */
    @GetMapping("/admin")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_MODERATOR')")
    public ResponseEntity<ApiResponse<Page<BlogResponse>>> getAllForAdmin(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(ApiResponse.success(blogService.getAllBlogsForAdmin(status, pageable)));
    }

    /**
     * Admin duyệt hoặc từ chối bài viết.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_MODERATOR')")
    public ResponseEntity<ApiResponse<BlogResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody BlogStatusRequest request) {

        User reviewer = securityUtils.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật trạng thái bài viết thành công",
                blogService.updateBlogStatus(id, request, reviewer)));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    private Pageable buildPageable(int page, int size, String sort) {
        try {
            String[] parts = sort.split(",");
            Sort.Direction dir = parts.length > 1 && "asc".equalsIgnoreCase(parts[1])
                    ? Sort.Direction.ASC : Sort.Direction.DESC;
            return PageRequest.of(page, size, Sort.by(dir, parts[0]));
        } catch (Exception e) {
            return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        }
    }
}
