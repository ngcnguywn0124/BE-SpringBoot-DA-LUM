package com.example.be_springboot_lum.service;

import com.example.be_springboot_lum.dto.response.FavoriteResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface FavoriteService {

    FavoriteResponse saveProduct(UUID productId);

    void unsaveProduct(UUID productId);

    Page<FavoriteResponse> getMyFavorites(String status, int page, int size);

    boolean isProductSaved(UUID productId);

    long countMyAvailableFavorites();
}
