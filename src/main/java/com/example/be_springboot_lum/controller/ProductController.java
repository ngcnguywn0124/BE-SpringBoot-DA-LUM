package com.example.be_springboot_lum.controller;

import com.example.be_springboot_lum.common.ApiResponse;
import com.example.be_springboot_lum.dto.request.ProductFilterRequest;
import com.example.be_springboot_lum.dto.request.ProductRequest;
import com.example.be_springboot_lum.dto.response.ProductResponse;
import com.example.be_springboot_lum.dto.response.ProductSummaryResponse;
import com.example.be_springboot_lum.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Product Controller – Quản lý tin đăng bán / trao đổi đồ cũ
 *
 * ┌─ [PUBLIC] ─────────────────────────────────────────────────────────────────────┐
 * │ GET  /api/v1/products                – Danh sách sản phẩm (lọc + phân trang)  │
 * │ GET  /api/v1/products/search         – Tìm kiếm theo từ khóa                  │
 * │ GET  /api/v1/products/trending       – Sản phẩm trending                      │
 * │ GET  /api/v1/products/{id}           – Chi tiết theo ID                       │
 * │ GET  /api/v1/products/slug/{slug}    – Chi tiết theo slug                     │
 * ├─ [AUTHENTICATED] ──────────────────────────────────────────────────────────────┤
 * │ POST   /api/v1/products              – Đăng tin mới (multipart)               │
 * │ PUT    /api/v1/products/{id}         – Cập nhật tin (multipart)               │
 * │ GET    /api/v1/products/my           – Tin của bản thân                       │
 * │ PATCH  /api/v1/products/{id}/sold    – Đánh dấu đã bán                       │
 * │ PATCH  /api/v1/products/{id}/toggle-hidden – Ẩn / hiện tin                   │
 * │ PATCH  /api/v1/products/{id}/primary-image/{imageId} – Đặt ảnh bìa          │
 * │ DELETE /api/v1/products/{id}         – Xóa mềm tin đăng                      │
 * ├─ [ADMIN / SUPER_ADMIN] ────────────────────────────────────────────────────────┤
 * │ GET    /api/v1/products/admin        – Tất cả tin (lọc trạng thái + keyword)  │
 * │ PATCH  /api/v1/products/{id}/approve – Duyệt tin (pending → available)        │
 * │ PATCH  /api/v1/products/{id}/hide    – Admin ẩn tin                           │
 * │ PATCH  /api/v1/products/{id}/feature – Bật / tắt nổi bật                     │
 * └────────────────────────────────────────────────────────────────────────────────┘
 */
@RestController
@RequestMapping("${api.prefix}/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // ═════════════════════════════════════════════════════════════════════════
    // PUBLIC
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Lấy danh sách sản phẩm available với các bộ lọc tùy chọn và phân trang.
     *
     * <pre>
     * GET /api/v1/products
     *   ?categoryId=uuid
     *   &universityId=uuid
     *   &campusId=uuid
     *   &listingType=sell|exchange|both
     *   &condition=new|like_new|used|old|broken
     *   &isFree=true|false
     *   &minPrice=0
     *   &maxPrice=1000000
     *   &page=0
     *   &size=20
     *   &sort=createdAt,desc
     * </pre>
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductSummaryResponse>>> getProducts(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID universityId,
            @RequestParam(required = false) UUID campusId,
            @RequestParam(required = false) String listingType,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) Boolean isFree,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        ProductFilterRequest filter = ProductFilterRequest.builder()
                .categoryId(categoryId)
                .universityId(universityId)
                .campusId(campusId)
                .listingType(listingType)
                .condition(condition)
                .isFree(isFree)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .page(page)
                .size(size)
                .sort(sort)
                .build();

        return ResponseEntity.ok(ApiResponse.success(productService.getProducts(filter)));
    }

    /**
     * Tìm kiếm sản phẩm theo từ khóa (title hoặc description).
     *
     * <pre>
     * GET /api/v1/products/search?keyword=laptop&page=0&size=20
     * </pre>
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<ProductSummaryResponse>>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.success(
                productService.searchProducts(keyword, page, size)));
    }

    /**
     * Sản phẩm trending trong 30 ngày gần nhất.
     *
     * <pre>
     * GET /api/v1/products/trending?page=0&size=20
     * </pre>
     */
    @GetMapping("/trending")
    public ResponseEntity<ApiResponse<Page<ProductSummaryResponse>>> getTrending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.success(
                productService.getTrendingProducts(page, size)));
    }

    /**
     * Chi tiết sản phẩm theo ID (tự động +1 view_count).
     *
     * <pre>
     * GET /api/v1/products/{id}
     * </pre>
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductById(id)));
    }

    /**
     * Chi tiết sản phẩm theo slug (SEO-friendly URL).
     *
     * <pre>
     * GET /api/v1/products/slug/laptop-dell-xps-15-abc123
     * </pre>
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<ProductResponse>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductBySlug(slug)));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // AUTHENTICATED – Người dùng
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Đăng tin mới.
     * Gửi multipart/form-data:
     *   - part "data"   : JSON của ProductRequest
     *   - part "images" : danh sách file ảnh (1–10 ảnh)
     *
     * <pre>
     * POST /api/v1/products  (Content-Type: multipart/form-data)
     * -F "data={...}"
     * -F "images=@/path/anh1.jpg"
     * -F "images=@/path/anh2.jpg"
     * </pre>
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestPart("data")   ProductRequest request,
            @RequestPart("images")        List<MultipartFile> images) {

        return ResponseEntity.status(201)
                .body(ApiResponse.created(productService.createProduct(request, images)));
    }

    /**
     * Cập nhật tin đăng (chỉ chủ sở hữu).
     * Nếu không gửi "images" thì ảnh cũ được giữ nguyên.
     *
     * <pre>
     * PUT /api/v1/products/{id}  (Content-Type: multipart/form-data)
     * -F "data={...}"
     * -F "images=@/path/anh_moi.jpg"   (tùy chọn)
     * </pre>
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestPart("data")   ProductRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {

        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật tin đăng thành công",
                productService.updateProduct(id, request, images)));
    }

    /**
     * Xem danh sách tin của bản thân (lọc theo trạng thái).
     *
     * <pre>
     * GET /api/v1/products/my?status=available&page=0&size=20
     * </pre>
     */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<ProductSummaryResponse>>> getMyProducts(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.success(
                productService.getMyProducts(status, page, size)));
    }

    /**
     * Đánh dấu sản phẩm là đã bán.
     *
     * <pre>
     * PATCH /api/v1/products/{id}/sold
     * </pre>
     */
    @PatchMapping("/{id}/sold")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProductResponse>> markAsSold(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đã đánh dấu bán thành công", productService.markAsSold(id)));
    }

    /**
     * Ẩn / hiện tin (toggle).
     * available → hidden hoặc hidden → available.
     *
     * <pre>
     * PATCH /api/v1/products/{id}/toggle-hidden
     * </pre>
     */
    @PatchMapping("/{id}/toggle-hidden")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProductResponse>> toggleHidden(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đã thay đổi hiển thị tin đăng", productService.toggleHidden(id)));
    }

    /**
     * Gia hạn tin đăng (tăng renewalCount và cập nhật expiresAt).
     *
     * <pre>
     * PATCH /api/v1/products/{id}/renew?days=30
     * </pre>
     */
    @PatchMapping("/{id}/renew")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ProductResponse>> renewProduct(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(ApiResponse.success(
                "Gia hạn tin đăng thành công", productService.renewProduct(id, days)));
    }

    /**
     * Xóa mềm tin đăng (chuyển status → deleted).
     *
     * <pre>
     * DELETE /api/v1/products/{id}
     * </pre>
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_MODERATOR')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa tin đăng", null));
    }

    /**
     * Xóa cứng tin đăng khỏi hệ thống (chỉ SUPER_ADMIN).
     *
     * <pre>
     * DELETE /api/v1/products/{id}/hard
     * </pre>
     */
    @DeleteMapping("/{id}/hard")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> hardDeleteProduct(@PathVariable UUID id) {
        productService.hardDeleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa vĩnh viễn tin đăng", null));
    }

    /**
     * Đặt một hình ảnh làm ảnh chính (is_primary = true).
     *
     * <pre>
     * PATCH /api/v1/products/{id}/primary-image/{imageId}
     * </pre>
     */
    @PatchMapping("/{id}/primary-image/{imageId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> setPrimaryImage(
            @PathVariable UUID id,
            @PathVariable UUID imageId) {
        productService.setPrimaryImage(id, imageId);
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật ảnh bìa", null));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ADMIN
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Admin xem tất cả tin (có lọc theo status và keyword).
     *
     * <pre>
     * GET /api/v1/products/admin?status=pending&keyword=laptop&page=0&size=20
     * </pre>
     */
    @GetMapping("/admin")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_MODERATOR')")
    public ResponseEntity<ApiResponse<Page<ProductSummaryResponse>>> getAllForAdmin(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.success(
                productService.getAllProductsForAdmin(status, keyword, page, size)));
    }

    /**
     * Admin duyệt tin (pending / hidden → available).
     *
     * <pre>
     * PATCH /api/v1/products/{id}/approve
     * </pre>
     */
    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_MODERATOR')")
    public ResponseEntity<ApiResponse<ProductResponse>> approveProduct(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đã duyệt tin đăng", productService.approveProduct(id)));
    }

    /**
     * Admin ẩn tin.
     *
     * <pre>
     * PATCH /api/v1/products/{id}/hide
     * </pre>
     */
    @PatchMapping("/{id}/hide")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_MODERATOR')")
    public ResponseEntity<ApiResponse<ProductResponse>> hideProduct(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đã ẩn tin đăng", productService.hideProduct(id)));
    }

    /**
     * Admin bật / tắt nổi bật.
     *
     * <pre>
     * PATCH /api/v1/products/{id}/feature
     * </pre>
     */
    @PatchMapping("/{id}/feature")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> toggleFeatured(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đã cập nhật trạng thái nổi bật", productService.toggleFeatured(id)));
    }

    /**
     * Admin khôi phục tin xóa mềm.
     *
     * <pre>
     * PATCH /api/v1/products/{id}/restore
     * </pre>
     */
    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> restoreProduct(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Đã khôi phục tin đăng", productService.restoreProduct(id)));
    }
}
