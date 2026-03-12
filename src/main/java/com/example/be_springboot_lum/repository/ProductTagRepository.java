package com.example.be_springboot_lum.repository;

import com.example.be_springboot_lum.model.ProductTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProductTagRepository extends JpaRepository<ProductTag, UUID> {

    List<ProductTag> findByProduct_ProductId(UUID productId);

    void deleteByProduct_ProductId(UUID productId);

    /** Giảm usage_count của các tag khi xóa liên kết */
    @Modifying
    @Query("UPDATE Tag t SET t.usageCount = GREATEST(t.usageCount - 1, 0) WHERE t.tagId IN :tagIds")
    void decrementUsageCountForTags(@Param("tagIds") List<UUID> tagIds);
}
