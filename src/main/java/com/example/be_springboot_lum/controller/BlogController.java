package com.example.be_springboot_lum.controller;

import com.example.be_springboot_lum.common.ApiResponse;
import com.example.be_springboot_lum.dto.request.BlogRequest;
import com.example.be_springboot_lum.dto.request.BlogStatusRequest;
import com.example.be_springboot_lum.dto.response.BlogResponse;
import com.example.be_springboot_lum.model.BlogStatus;
import com.example.be_springboot_lum.service.BlogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("${api.prefix}/blogs")
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<BlogResponse>>> getAllBlogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isFeatured) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách bài viết thành công",
                blogService.getAllBlogs(page, size, categoryId, search, BlogStatus.published, isFeatured)));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'MODERATOR')")
    public ResponseEntity<ApiResponse<Page<BlogResponse>>> getAllBlogsForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BlogStatus status,
            @RequestParam(required = false) Boolean isFeatured) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh sách bài viết thành công (Admin)",
                blogService.getAllBlogs(page, size, categoryId, search, status, isFeatured)));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<BlogResponse>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy chi tiết bài viết thành công",
                blogService.getBlogBySlug(slug)));
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<ApiResponse<BlogResponse>> getById(@PathVariable UUID id) {
        // Có thể mở rộng logic kiểm tra role ở đây nếu cần trả về nội dung nháp cho Admin
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy chi tiết bài viết thành công",
                blogService.getBlogById(id, false)));
    }

    @PostMapping(value = "/upload-image", consumes = { "multipart/form-data" })
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<String>> uploadBlogImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.<String>builder()
                    .code(400)
                    .message("Vui lòng chọn file ảnh")
                    .build());
        }
        return ResponseEntity.ok(ApiResponse.success(
                "Tải ảnh lên thành công",
                blogService.uploadBlogImage(file)));
    }

    @PostMapping(consumes = { "multipart/form-data" })
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<BlogResponse>> createBlog(@Valid @ModelAttribute BlogRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.created(
                blogService.createBlog(request)));
    }

    @PutMapping(value = "/{id}", consumes = { "multipart/form-data" })
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<BlogResponse>> updateBlog(
            @PathVariable UUID id,
            @Valid @ModelAttribute BlogRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật bài viết thành công",
                blogService.updateBlog(id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<BlogResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody BlogStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật trạng thái bài viết thành công",
                blogService.updateBlogStatus(id, request.getStatus())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBlog(@PathVariable UUID id) {
        blogService.deleteBlog(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa bài viết thành công", null));
    }
}
