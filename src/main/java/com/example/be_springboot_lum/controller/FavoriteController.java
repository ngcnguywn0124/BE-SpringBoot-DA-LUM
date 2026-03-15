package com.example.be_springboot_lum.controller;

import com.example.be_springboot_lum.common.ApiResponse;
import com.example.be_springboot_lum.dto.response.FavoriteResponse;
import com.example.be_springboot_lum.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("${api.prefix}/favorites")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/{productId}")
    public ResponseEntity<ApiResponse<FavoriteResponse>> saveProduct(@PathVariable UUID productId) {
        return ResponseEntity.status(201)
                .body(ApiResponse.created(favoriteService.saveProduct(productId)));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> unsaveProduct(@PathVariable UUID productId) {
        favoriteService.unsaveProduct(productId);
        return ResponseEntity.ok(ApiResponse.success("Bỏ lưu tin thành công", null));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<FavoriteResponse>>> getMyFavorites(
            @RequestParam(required = false, defaultValue = "all") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.success(
                favoriteService.getMyFavorites(status, page, size)));
    }

    @GetMapping("/check/{productId}")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> isSaved(@PathVariable UUID productId) {
        boolean saved = favoriteService.isProductSaved(productId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("saved", saved)));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> countFavorites() {
        return ResponseEntity.ok(ApiResponse.success(favoriteService.countMyAvailableFavorites()));
    }
}
