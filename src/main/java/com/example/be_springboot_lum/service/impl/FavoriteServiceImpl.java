package com.example.be_springboot_lum.service.impl;

import com.example.be_springboot_lum.dto.response.FavoriteResponse;
import com.example.be_springboot_lum.dto.response.ProductSummaryResponse;
import com.example.be_springboot_lum.exception.AppException;
import com.example.be_springboot_lum.exception.ErrorCode;
import com.example.be_springboot_lum.model.Favorite;
import com.example.be_springboot_lum.model.Product;
import com.example.be_springboot_lum.model.ProductImage;
import com.example.be_springboot_lum.model.User;
import com.example.be_springboot_lum.repository.FavoriteRepository;
import com.example.be_springboot_lum.repository.ProductRepository;
import com.example.be_springboot_lum.service.FavoriteService;
import com.example.be_springboot_lum.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private static final Set<String> NON_SAVEABLE_STATUSES = Set.of("deleted", "pending");

    private final FavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public FavoriteResponse saveProduct(UUID productId) {
        UUID userId = securityUtils.getCurrentUserId();
        
        // Handle "Toggle" behavior: if already exists, return existing instead of 409
        return favoriteRepository.findByUser_UserIdAndProduct_ProductId(userId, productId)
                .map(favorite -> toResponse(favorite, userId))
                .orElseGet(() -> {
                    Product product = productRepository.findById(productId)
                            .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

                    if (NON_SAVEABLE_STATUSES.contains(product.getStatus())) {
                        throw new AppException(ErrorCode.PRODUCT_NOT_AVAILABLE_FOR_FAVORITE);
                    }

                    User currentUser = securityUtils.getCurrentUser();
                    Favorite favorite = favoriteRepository.save(Favorite.builder()
                            .user(currentUser)
                            .product(product)
                            .build());

                    return toResponse(favorite, userId);
                });
    }

    @Override
    @Transactional
    public void unsaveProduct(UUID productId) {
        UUID userId = securityUtils.getCurrentUserId();

        Favorite favorite = favoriteRepository.findByUser_UserIdAndProduct_ProductId(userId, productId)
                .orElseThrow(() -> new AppException(ErrorCode.FAVORITE_NOT_FOUND));

        favoriteRepository.delete(favorite);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FavoriteResponse> getMyFavorites(String status, int page, int size) {
        UUID userId = securityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Favorite> favorites;
        if (status == null || status.isBlank() || "all".equalsIgnoreCase(status)) {
            favorites = favoriteRepository.findByUserId(userId, pageable);
        } else if ("active".equalsIgnoreCase(status)) {
            favorites = favoriteRepository.findActiveFavoritesByUserId(userId, pageable);
        } else {
            favorites = favoriteRepository.findByUserIdAndProductStatus(userId, status, pageable);
        }

        return favorites.map(f -> toResponse(f, userId));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isProductSaved(UUID productId) {
        UUID userId = securityUtils.getCurrentUserId();
        return favoriteRepository.existsByUser_UserIdAndProduct_ProductId(userId, productId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countMyAvailableFavorites() {
        UUID userId = securityUtils.getCurrentUserId();
        return favoriteRepository.countActiveByUserId(userId);
    }

    private FavoriteResponse toResponse(Favorite favorite, UUID currentUserId) {
        Product product = favorite.getProduct();
        return FavoriteResponse.builder()
                .favoriteId(favorite.getFavoriteId())
                .productId(product.getProductId())
                .savedAt(favorite.getCreatedAt())
                .product(toSummary(product, currentUserId))
                .build();
    }

    private ProductSummaryResponse toSummary(Product p, UUID currentUserId) {
        String thumbnail = p.getImages().stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElse(p.getImages().isEmpty() ? null : p.getImages().get(0).getImageUrl());

        return ProductSummaryResponse.builder()
                .productId(p.getProductId())
                .title(p.getTitle())
                .slug(p.getSlug())
                .condition(p.getCondition())
                .price(p.getPrice())
                .isFree(p.getIsFree())
                .isNegotiable(p.getIsNegotiable())
                .listingType(p.getListingType())
                .status(p.getStatus())
                .previousStatus(p.getPreviousStatus())
                .viewCount(p.getViewCount())
                .favoriteCount(p.getFavoriteCount())
                .isFeatured(p.getIsFeatured())
                .imageCount(p.getImages() != null ? p.getImages().size() : 0)
                .renewalCount(p.getRenewalCount())
                .createdAt(p.getCreatedAt())
                .approvedAt(p.getApprovedAt())
                .expiresAt(p.getExpiresAt())
                .thumbnailUrl(thumbnail)
                .categoryId(p.getCategory() != null ? p.getCategory().getCategoryId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getCategoryName() : null)
                .categorySlug(p.getCategory() != null ? p.getCategory().getSlug() : null)
                .universityShortName(p.getUniversity() != null ? p.getUniversity().getShortName() : null)
                .campusName(p.getCampus() != null ? p.getCampus().getCampusName() : null)
                .sellerId(p.getSeller() != null ? p.getSeller().getUserId() : null)
                .sellerName(p.getSeller() != null ? p.getSeller().getFullName() : null)
                .sellerAvatar(p.getSeller() != null ? p.getSeller().getAvatarUrl() : null)
                .isFavorited(favoriteRepository.existsByUser_UserIdAndProduct_ProductId(currentUserId, p.getProductId()))
                .build();
    }
}
