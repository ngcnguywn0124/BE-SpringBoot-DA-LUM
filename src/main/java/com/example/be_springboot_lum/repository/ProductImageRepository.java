package com.example.be_springboot_lum.repository;

import com.example.be_springboot_lum.model.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    List<ProductImage> findByProduct_ProductIdOrderByDisplayOrderAscCreatedAtAsc(UUID productId);

    void deleteByProduct_ProductId(UUID productId);

    /** Đặt tất cả ảnh về is_primary = false trước khi set ảnh chính mới */
    @Modifying
    @Query("UPDATE ProductImage pi SET pi.isPrimary = false WHERE pi.product.productId = :productId")
    void clearPrimaryByProductId(@Param("productId") UUID productId);
}
