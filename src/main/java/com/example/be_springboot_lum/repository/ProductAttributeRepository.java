package com.example.be_springboot_lum.repository;

import com.example.be_springboot_lum.model.ProductAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductAttributeRepository extends JpaRepository<ProductAttribute, UUID> {

    /** Lấy tất cả thuộc tính của một danh mục, sắp theo display_order rồi tên */
    List<ProductAttribute> findByCategory_CategoryIdOrderByDisplayOrderAscAttributeNameAsc(UUID categoryId);

    /** Kiểm tra tên thuộc tính đã tồn tại trong danh mục chưa */
    boolean existsByAttributeNameIgnoreCaseAndCategory_CategoryId(String attributeName, UUID categoryId);

    /** Kiểm tra tên trùng, bỏ qua chính entity đang sửa */
    boolean existsByAttributeNameIgnoreCaseAndCategory_CategoryIdAndAttributeIdNot(
            String attributeName, UUID categoryId, UUID excludeId);

    /** Đếm số lượng thuộc tính của danh mục */
    long countByCategory_CategoryId(UUID categoryId);
}
