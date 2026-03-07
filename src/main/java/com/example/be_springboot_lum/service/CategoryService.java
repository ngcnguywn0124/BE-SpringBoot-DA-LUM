package com.example.be_springboot_lum.service;

import com.example.be_springboot_lum.dto.request.CategoryRequest;
import com.example.be_springboot_lum.dto.response.CategoryResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface CategoryService {

    /** Lấy tất cả danh mục gốc kèm danh mục con (dạng cây) */
    List<CategoryResponse> getCategoryTree();

    /** Lấy danh mục gốc hoặc theo từ khóa (phẳng, không kèm con) */
    List<CategoryResponse> getAllCategories(String keyword);

    /** Lấy danh mục con trực tiếp của một cha */
    List<CategoryResponse> getChildCategories(UUID parentId);

    /** Chi tiết một danh mục (kèm con trực tiếp) */
    CategoryResponse getCategoryById(UUID id);

    /** Tạo danh mục mới (có thể kèm ảnh) */
    CategoryResponse createCategory(CategoryRequest request, MultipartFile image);

    /** Cập nhật danh mục (thay ảnh nếu cung cấp file mới) */
    CategoryResponse updateCategory(UUID id, CategoryRequest request, MultipartFile image);

    /** Chỉ cập nhật ảnh của danh mục */
    CategoryResponse updateCategoryImage(UUID id, MultipartFile image);

    /** Xóa danh mục (phải không có sản phẩm và không có con) */
    void deleteCategory(UUID id);
}
