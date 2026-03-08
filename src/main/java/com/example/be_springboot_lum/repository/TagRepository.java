package com.example.be_springboot_lum.repository;

import com.example.be_springboot_lum.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TagRepository extends JpaRepository<Tag, UUID> {

    /** Tìm kiếm tag theo tên (không phân biệt hoa/thường) */
    List<Tag> findByTagNameContainingIgnoreCaseOrderByUsageCountDescTagNameAsc(String keyword);

    /** Tất cả tag – sắp theo lượt dùng giảm dần */
    List<Tag> findAllByOrderByUsageCountDescTagNameAsc();

    /** Kiểm tra tên tồn tại */
    boolean existsByTagNameIgnoreCase(String tagName);

    /** Kiểm tra tên trùng, bỏ qua chính entity đang sửa */
    boolean existsByTagNameIgnoreCaseAndTagIdNot(String tagName, UUID excludeId);

    Optional<Tag> findBySlug(String slug);

    Optional<Tag> findByTagNameIgnoreCase(String tagName);
}
