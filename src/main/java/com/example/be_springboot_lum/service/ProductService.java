package com.example.be_springboot_lum.service;

import com.example.be_springboot_lum.dto.request.ProductFilterRequest;
import com.example.be_springboot_lum.dto.request.ProductRequest;
import com.example.be_springboot_lum.dto.response.ProductResponse;
import com.example.be_springboot_lum.dto.response.ProductSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ProductService {

    // ─── Public queries ───────────────────────────────────────────────────────

    /** Danh sách sản phẩm available có lọc + phân trang */
    Page<ProductSummaryResponse> getProducts(ProductFilterRequest filter);

    /** Tìm kiếm theo từ khóa */
    Page<ProductSummaryResponse> searchProducts(String keyword, int page, int size);

    /** Chi tiết sản phẩm theo ID (tự động tăng view_count) */
    ProductResponse getProductById(UUID productId);

    /** Chi tiết sản phẩm theo slug (tự động tăng view_count) */
    ProductResponse getProductBySlug(String slug);

    /** Sản phẩm trending (30 ngày gần nhất) */
    Page<ProductSummaryResponse> getTrendingProducts(int page, int size);

    /** Tự động cập nhật các tin đã quá hạn */
    void checkAndExpireProducts();

    // ─── Authenticated – người dùng thường ───────────────────────────────────

    /** Tạo tin đăng mới */
    ProductResponse createProduct(ProductRequest request, List<MultipartFile> images);

    /** Cập nhật tin đăng (chỉ chủ sở hữu) */
    ProductResponse updateProduct(UUID productId, ProductRequest request, List<MultipartFile> newImages);

    /** Xem danh sách tin của bản thân (có lọc theo status) */
    Page<ProductSummaryResponse> getMyProducts(String status, int page, int size);

    /** Đánh dấu sản phẩm là đã bán */
    ProductResponse markAsSold(UUID productId);

    /** Ẩn / hiện tin (toggle hidden) */
    ProductResponse toggleHidden(UUID productId);

    /** Gia hạn tin đăng */
    ProductResponse renewProduct(UUID productId, int days);

    /** Xóa mềm tin đăng (chuyển sang status = 'deleted') */
    void deleteProduct(UUID productId);

    /** Xóa cứng tin đăng khỏi hệ thống (chỉ dành cho Super Admin) */
    void hardDeleteProduct(UUID productId);

    /** Đặt một ảnh làm ảnh chính (is_primary = true) */
    void setPrimaryImage(UUID productId, UUID imageId);

    // ─── Admin ────────────────────────────────────────────────────────────────

    /** Admin xem tất cả tin (lọc theo status + keyword) */
    Page<ProductSummaryResponse> getAllProductsForAdmin(String status, String keyword, int page, int size);

    /** Admin duyệt tin (pending → available) */
    ProductResponse approveProduct(UUID productId);

    /** Admin ẩn tin */
    ProductResponse hideProduct(UUID productId);

    /** Admin bật / tắt nổi bật */
    ProductResponse toggleFeatured(UUID productId);
}
