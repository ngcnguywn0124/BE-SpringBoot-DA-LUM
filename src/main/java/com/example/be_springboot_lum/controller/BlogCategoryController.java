package com.example.be_springboot_lum.controller;

import com.example.be_springboot_lum.common.ApiResponse;
import com.example.be_springboot_lum.dto.request.BlogCategoryRequest;
import com.example.be_springboot_lum.dto.response.BlogCategoryResponse;
import com.example.be_springboot_lum.service.BlogCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${api.prefix}/blog-categories")
@RequiredArgsConstructor
public class BlogCategoryController {

    private final BlogCategoryService blogCategoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BlogCategoryResponse>>> getAllCategories(
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh mục blog thành công",
                blogCategoryService.getAllCategories(keyword, true)));
    }

    @GetMapping("/admin/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<BlogCategoryResponse>>> getAllCategoriesForAdmin(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy danh mục blog thành công (Admin)",
                blogCategoryService.getAllCategories(keyword, isActive)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BlogCategoryResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Lấy chi tiết danh mục blog thành công",
                blogCategoryService.getCategoryById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<BlogCategoryResponse>> createCategory(
            @Valid @RequestBody BlogCategoryRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.created(
                blogCategoryService.createCategory(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<BlogCategoryResponse>> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody BlogCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật danh mục blog thành công",
                blogCategoryService.updateCategory(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable UUID id) {
        blogCategoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa danh mục blog thành công", null));
    }
}
