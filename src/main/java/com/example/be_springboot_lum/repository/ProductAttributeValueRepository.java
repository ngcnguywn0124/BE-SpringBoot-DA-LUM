package com.example.be_springboot_lum.repository;

import com.example.be_springboot_lum.model.ProductAttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductAttributeValueRepository extends JpaRepository<ProductAttributeValue, UUID> {

    List<ProductAttributeValue> findByProduct_ProductId(UUID productId);

    void deleteByProduct_ProductId(UUID productId);
}
