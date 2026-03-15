package com.example.be_springboot_lum.service.impl;

import com.example.be_springboot_lum.dto.request.ProductAttributeValueRequest;
import com.example.be_springboot_lum.dto.request.ProductFilterRequest;
import com.example.be_springboot_lum.dto.request.ProductRequest;
import com.example.be_springboot_lum.dto.response.*;
import com.example.be_springboot_lum.exception.AppException;
import com.example.be_springboot_lum.exception.ErrorCode;
import com.example.be_springboot_lum.model.*;
import com.example.be_springboot_lum.repository.*;
import com.example.be_springboot_lum.service.CloudinaryService;
import com.example.be_springboot_lum.service.ProductService;
import com.example.be_springboot_lum.util.SecurityUtils;
import com.example.be_springboot_lum.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository              productRepository;
    private final ProductImageRepository         productImageRepository;
    private final ProductAttributeValueRepository attributeValueRepository;
    private final ProductTagRepository           productTagRepository;
    private final CategoryRepository             categoryRepository;
    private final ProductAttributeRepository     productAttributeRepository;
    private final TagRepository                  tagRepository;
    private final FavoriteRepository             favoriteRepository;
    private final UniversityRepository           universityRepository;
    private final CampusRepository               campusRepository;
    private final CloudinaryService              cloudinaryService;
    private final SecurityUtils                  securityUtils;
    private final SimpMessagingTemplate          messagingTemplate;

    @Value("${cloudinary.upload-folder:lum}")
    private String baseFolder;

    private static final String PRODUCT_FOLDER_SUFFIX = "/products";
    private static final int    MAX_IMAGES            = 10;

    // ═════════════════════════════════════════════════════════════════════════
    // Public queries
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> getProducts(ProductFilterRequest filter) {
        Pageable pageable = buildPageable(filter.getPage(), filter.getSize(), filter.getSort());
        return productRepository.findAvailableWithFilters(
                        filter.getCategoryId(),
                        filter.getUniversityId(),
                        filter.getCampusId(),
                        filter.getListingType(),
                        filter.getCondition(),
                        filter.getIsFree(),
                        filter.getMinPrice(),
                        filter.getMaxPrice(),
                        pageable)
                .map(this::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> searchProducts(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return productRepository.searchByKeyword(keyword, pageable).map(this::toSummary);
    }

    @Override
    @Transactional
    public ProductResponse getProductById(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        productRepository.incrementViewCount(productId);
        return toFullResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlugAndStatusNot(slug, "deleted")
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        productRepository.incrementViewCount(product.getProductId());
        return toFullResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> getTrendingProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        OffsetDateTime since = OffsetDateTime.now().minusDays(30);
        return productRepository.findTrending(since, pageable).map(this::toSummary);
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 * * * *") // Chạy mỗi giờ (tại phút 0)
    public void checkAndExpireProducts() {
        // Tìm các tin sắp bị expired để gửi thông báo trước khi query update tập thể
        List<Product> nearingExpiry = productRepository.findAllByStatusAndExpiresAtBefore("available", OffsetDateTime.now());
        
        int updated = productRepository.updateExpiredProducts();
        if (updated > 0) {
            log.info("Đã cập nhật trạng thái 'expired' cho {} tin đăng đã quá hạn", updated);
            
            // Gửi thông báo realtime cho từng chủ tin
            for (Product p : nearingExpiry) {
                String destination = "/topic/user-" + p.getSeller().getUserId();
                messagingTemplate.convertAndSend(destination, "PRODUCT_EXPIRED:" + p.getProductId());
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Authenticated – users
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request, List<MultipartFile> images) {
        // Validate ảnh
        if (CollectionUtils.isEmpty(images)) {
            throw new AppException(ErrorCode.PRODUCT_IMAGE_REQUIRED);
        }
        if (images.size() > MAX_IMAGES) {
            throw new AppException(ErrorCode.PRODUCT_IMAGE_LIMIT_EXCEEDED);
        }

        // Validate giá
        validatePrice(request);

        User seller = securityUtils.getCurrentUser();

        // Resolve danh mục
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        // Resolve trường / cơ sở (nếu có)
        University university = resolveUniversity(request.getUniversityId());
        Campus campus = resolveCampus(request.getCampusId());

        // Slug unique
        String slug = generateUniqueSlug(request.getTitle());

        // Build product
        Product product = Product.builder()
                .seller(seller)
                .category(category)
                .university(university)
                .campus(campus)
                    .title(request.getTitle())
                .description(request.getDescription())
                .slug(slug)
                    .condition(request.getCondition())
                .price(request.getPrice())
                .isFree(request.getIsFree() != null ? request.getIsFree() : false)
                .isNegotiable(request.getIsNegotiable() != null ? request.getIsNegotiable() : true)
                .listingType(request.getListingType() != null ? request.getListingType() : "sell")
                .exchangePreferences(request.getExchangePreferences())
                .transactionType(request.getTransactionType() != null ? request.getTransactionType() : "meetup")
                .meetingPoint(request.getMeetingPoint())
                .contactName(request.getContactName())
                .contactPhone(request.getContactPhone())
                .zaloLink(request.getZaloLink())
                .facebookLink(request.getFacebookLink())
                .status("pending") // chờ admin duyệt
                .expiryDays(request.getExpireDays() != null ? request.getExpireDays() : 30)
                .expiresAt(OffsetDateTime.now().plusDays(request.getExpireDays() != null ? request.getExpireDays() : 30))
                .build();

        product = productRepository.save(product);

        // Upload và lưu ảnh
        saveImages(product, images);

        // Lưu thuộc tính động
        saveAttributeValues(product, request.getAttributeValues());

        // Lưu tags
        saveTags(product, request.getTagIds(), request.getNewTagNames());

        ProductResponse response = toFullResponse(productRepository.findById(product.getProductId()).orElseThrow());

        // Gửi thông báo realtime cho admin khi có tin mới
        try {
            messagingTemplate.convertAndSend("/topic/admin/products", "NEW_PRODUCT_CREATED");
        } catch (Exception e) {
            log.warn("Không thể gửi thông báo WebSocket cho admin về tin mới: {}", e.getMessage());
        }

        return response;
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(UUID productId, ProductRequest request, List<MultipartFile> newImages) {
        Product product = getOwnedProduct(productId);

        // Không cho sửa tin đã xóa, đã hết hạn.
        // Nếu bị admin ẩn: chỉ cho sửa nếu trạng thái trước đó là 'pending' (đang chờ duyệt)
        if ("deleted".equals(product.getStatus())) {
            throw new AppException(ErrorCode.PRODUCT_ALREADY_DELETED);
        }
        if ("expired".equals(product.getStatus())) {
            throw new AppException(ErrorCode.PRODUCT_NOT_EXPIRED);
        }

        if ("admin_hidden".equals(product.getStatus())) {
            // Chỉ cho phép sửa nếu trước khi bị ẩn, tin đó đang ở trạng thái 'pending'
            if (!"pending".equals(product.getPreviousStatus())) {
                throw new AppException(ErrorCode.ACCESS_DENIED);
            }
        }

        // Validate giá
        validatePrice(request);

        // Cập nhật danh mục nếu thay đổi
        if (request.getCategoryId() != null && !request.getCategoryId().equals(product.getCategory().getCategoryId())) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
            product.setCategory(category);
        }

        // Cập nhật trường / cơ sở
        if (request.getUniversityId() != null) {
            product.setUniversity(resolveUniversity(request.getUniversityId()));
        }
        if (request.getCampusId() != null) {
            product.setCampus(resolveCampus(request.getCampusId()));
        }

        // Cập nhật các trường đơn giản
        if (request.getTitle() != null) {
            product.setTitle(request.getTitle());
            // Chỉ tạo slug mới nếu tiêu đề thay đổi
            product.setSlug(generateUniqueSlug(request.getTitle()));
        }
        if (request.getDescription()       != null) product.setDescription(request.getDescription());
        if (request.getCondition()         != null) product.setCondition(request.getCondition());
        if (request.getPrice()             != null) product.setPrice(request.getPrice());
        if (request.getIsFree()            != null) product.setIsFree(request.getIsFree());
        if (request.getIsNegotiable()      != null) product.setIsNegotiable(request.getIsNegotiable());
        if (request.getListingType()       != null) product.setListingType(request.getListingType());
        if (request.getExchangePreferences() != null) product.setExchangePreferences(request.getExchangePreferences());
        if (request.getTransactionType()   != null) product.setTransactionType(request.getTransactionType());
        if (request.getMeetingPoint()      != null) product.setMeetingPoint(request.getMeetingPoint());
        if (request.getContactName()       != null) product.setContactName(request.getContactName());
        if (request.getContactPhone()      != null) product.setContactPhone(request.getContactPhone());
        if (request.getZaloLink()          != null) product.setZaloLink(request.getZaloLink());
        if (request.getFacebookLink()      != null) product.setFacebookLink(request.getFacebookLink());

        // Cập nhật thời hạn
        if (request.getExpireDays() != null) {
            // Kiểm tra số lần gia hạn nếu thời hạn tăng lên so với cũ
            if (product.getExpiryDays() != null && request.getExpireDays() > product.getExpiryDays()) {
                if (product.getRenewalCount() >= 3) {
                    throw new AppException(ErrorCode.PRODUCT_RENEWAL_LIMIT_EXCEEDED);
                }
                product.setRenewalCount(product.getRenewalCount() + 1);
            }
            product.setExpiryDays(request.getExpireDays());
            product.setExpiresAt(OffsetDateTime.now().plusDays(request.getExpireDays())); // Tính từ thời điểm cập nhật
        }

        // Khi sửa tin → về lại pending để duyệt
        if ("available".equals(product.getStatus()) || "admin_hidden".equals(product.getStatus())) {
            product.setStatus("pending");
            product.setPreviousStatus(null); // Xóa previousStatus sau khi đã quay lại pending
        }

        productRepository.save(product);

        // Nếu có ảnh mới → thay thế toàn bộ ảnh cũ
        if (!CollectionUtils.isEmpty(newImages)) {
            if (newImages.size() > MAX_IMAGES) {
                throw new AppException(ErrorCode.PRODUCT_IMAGE_LIMIT_EXCEEDED);
            }
            deleteAllImages(product);
            saveImages(product, newImages);
        }

        // Cập nhật thuộc tính động
        if (request.getAttributeValues() != null) {
            attributeValueRepository.deleteByProduct_ProductId(product.getProductId());
            attributeValueRepository.flush(); // Flush để tránh lỗi Duplicate Key khi lưu mới các thuộc tính
            saveAttributeValues(product, request.getAttributeValues());
        }

        // Cập nhật tags
        if (request.getTagIds() != null || request.getNewTagNames() != null) {
            removeTags(product);
            saveTags(product, request.getTagIds(), request.getNewTagNames());
        }

        return toFullResponse(productRepository.findById(product.getProductId()).orElseThrow());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> getMyProducts(String status, int page, int size) {
        UUID userId = securityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Product> result = (status != null && !status.isBlank())
                ? productRepository.findBySeller_UserIdAndStatusOrderByCreatedAtDesc(userId, status, pageable)
                : productRepository.findBySeller_UserIdAndStatusNotOrderByCreatedAtDesc(userId, "deleted", pageable);
        return result.map(this::toSummary);
    }

    @Override
    @Transactional
    public ProductResponse markAsSold(UUID productId) {
        Product product = getOwnedProduct(productId);
        product.setStatus("sold");
        product.setSoldAt(OffsetDateTime.now());
        return toFullResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse toggleHidden(UUID productId) {
        Product product = getOwnedProduct(productId);
        if ("deleted".equals(product.getStatus())) {
            throw new AppException(ErrorCode.PRODUCT_ALREADY_DELETED);
        }
        // Nếu tin bị admin ẩn, người dùng không thể tự unhide
        if ("admin_hidden".equals(product.getStatus())) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        product.setStatus("hidden".equals(product.getStatus()) ? "available" : "hidden");
        return toFullResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse renewProduct(UUID productId, int days) {
        Product product = getOwnedProduct(productId);

        // Chỉ được gia hạn khi tin đã hết hạn
        if (!"expired".equals(product.getStatus())) {
            throw new AppException(ErrorCode.PRODUCT_NOT_EXPIRED);
        }

        if (product.getRenewalCount() >= 3) {
            throw new AppException(ErrorCode.PRODUCT_RENEWAL_LIMIT_EXCEEDED);
        }

        // Cập nhật trạng thái về available
        product.setStatus("available");

        product.setRenewalCount(product.getRenewalCount() + 1);
        product.setExpiryDays(days);
        // Gia hạn tính từ thời điểm hiện tại
        product.setExpiresAt(OffsetDateTime.now().plusDays(days));

        return toFullResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public void deleteProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        // Nếu là người dùng thường thì phải kiểm tra quyền sở hữu
        if (!securityUtils.hasRole("ROLE_ADMIN") && !securityUtils.hasRole("ROLE_SUPER_ADMIN") && !securityUtils.hasRole("ROLE_MODERATOR")) {
            UUID currentUserId = securityUtils.getCurrentUserId();
            if (!product.getSeller().getUserId().equals(currentUserId)) {
                throw new AppException(ErrorCode.PRODUCT_FORBIDDEN);
            }
        }

        if ("deleted".equals(product.getStatus())) {
            throw new AppException(ErrorCode.PRODUCT_ALREADY_DELETED);
        }

        product.setStatus("deleted");
        productRepository.save(product);
    }

    @Override
    @Transactional
    public void hardDeleteProduct(UUID productId) {
        if (!productRepository.existsById(productId)) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        
        // Xóa thuộc tính, tags, ảnh (DB + Cloudinary) trước khi xóa product
        attributeValueRepository.deleteByProduct_ProductId(productId);
        productTagRepository.deleteByProduct_ProductId(productId);
        
        // Lấy lại entity để xóa ảnh
        Product product = productRepository.findById(productId).get();
        deleteAllImages(product);
        
        productRepository.deleteById(productId);
    }

    @Override
    @Transactional
    public void setPrimaryImage(UUID productId, UUID imageId) {
        // Kiểm tra quyền sở hữu sản phẩm
        Product product = getOwnedProduct(productId);

        // Lấy tất cả ảnh hiện có của sản phẩm
        List<ProductImage> allImages = productImageRepository.findByProduct_ProductIdOrderByDisplayOrderAscCreatedAtAsc(productId);

        // Tìm ảnh cần set làm primary
        ProductImage targetImage = allImages.stream()
                .filter(img -> img.getImageId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.IMAGE_NOT_FOUND));

        // 1. Reset isPrimary của tất cả ảnh
        for (ProductImage img : allImages) {
            img.setIsPrimary(false);
        }

        // 2. Cập nhật ảnh đích lên đầu (displayOrder = 0) và set isPrimary
        targetImage.setIsPrimary(true);
        targetImage.setDisplayOrder(0);

        // 3. Cập nhật lại displayOrder cho các ảnh còn lại
        int order = 1;
        for (ProductImage img : allImages) {
            if (!img.getImageId().equals(imageId)) {
                img.setDisplayOrder(order++);
            }
        }

        productImageRepository.saveAll(allImages);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Admin
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> getAllProductsForAdmin(String status, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        String cleanStatus = (status != null && !status.trim().isEmpty()) ? status.trim() : null;
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;

        return productRepository.findAllForAdmin(
                cleanStatus,
                cleanKeyword,
                pageable).map(this::toSummary);
    }

    @Override
    @Transactional
    public ProductResponse approveProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        // Cho phép duyệt tin đang đợi, tin bị ẩn hoặc tin bị admin ẩn
        if (!"pending".equals(product.getStatus()) && !"hidden".equals(product.getStatus()) && !"admin_hidden".equals(product.getStatus())) {
            throw new AppException(ErrorCode.PRODUCT_STATUS_INVALID_TRANSITION);
        }

        // Nếu admin 'mở khóa' tin bị admin_hidden
        if ("admin_hidden".equals(product.getStatus())) {
            // Quay lại trạng thái trước đó (previousStatus)
            String restoreStatus = (product.getPreviousStatus() != null) ? product.getPreviousStatus() : "available";
            product.setStatus(restoreStatus);
            // Nếu trạng thái phục hồi là available, mới set approvedAt
            if ("available".equals(restoreStatus)) {
                product.setApprovedAt(OffsetDateTime.now());
            }
        } else {
            // Duyệt tin mới (pending -> available)
            product.setStatus("available");
            product.setApprovedAt(OffsetDateTime.now());
        }

        Product saved = productRepository.save(product);

        // Gửi event realtime cho chủ tin và trang chi tiết sản phẩm
        publishProductStatusEvent(
                saved.getProductId(),
                saved.getSeller().getUserId(),
                saved.getStatus(),
                "Tin đăng của bạn đã được duyệt và hiển thị trên hệ thống."
        );

        return toFullResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponse hideProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if ("deleted".equals(product.getStatus())) {
            throw new AppException(ErrorCode.PRODUCT_ALREADY_DELETED);
        }

        // Lưu trạng thái hiện tại trước khi admin ẩn
        if (!"admin_hidden".equals(product.getStatus())) {
            product.setPreviousStatus(product.getStatus());
        }

        // Admin ẩn sẽ là admin_hidden
        product.setStatus("admin_hidden");
        Product saved = productRepository.save(product);

        // Gửi event realtime cho chủ tin và trang chi tiết sản phẩm
        publishProductStatusEvent(
                saved.getProductId(),
                saved.getSeller().getUserId(),
                saved.getStatus(),
                "Tin đăng của bạn đã bị ẩn bởi quản trị viên."
        );

        return toFullResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponse toggleFeatured(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        product.setIsFeatured(!Boolean.TRUE.equals(product.getIsFeatured()));
        return toFullResponse(productRepository.save(product));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Private helpers
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Gửi sự kiện thay đổi trạng thái tin đăng qua WebSocket.
     * - Gửi riêng tư cho chủ tin: /user/{ownerId}/queue/notifications
     * - Gửi công khai cho trang chi tiết:  /topic/products/{productId}
     */
    private void publishProductStatusEvent(UUID productId, UUID ownerId, String newStatus, String message) {
        String json = String.format(
                "{\"type\":\"PRODUCT_STATUS_CHANGED\",\"productId\":\"%s\",\"newStatus\":\"%s\",\"message\":\"%s\"}",
                productId, newStatus, message
        );
        try {
            messagingTemplate.convertAndSend("/topic/user-" + ownerId, json);
            messagingTemplate.convertAndSend("/topic/products/" + productId, json);
        } catch (Exception e) {
            log.warn("Không thể gửi WebSocket event cho sản phẩm {}: {}", productId, e.getMessage());
        }
    }

    /** Lấy product và kiểm tra quyền sở hữu */
    private Product getOwnedProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        UUID currentUserId = securityUtils.getCurrentUserId();
        if (!product.getSeller().getUserId().equals(currentUserId)) {
            throw new AppException(ErrorCode.PRODUCT_FORBIDDEN);
        }
        return product;
    }

    /** Validate: phải có giá hoặc isFree = true */
    private void validatePrice(ProductRequest request) {
        boolean hasFree  = Boolean.TRUE.equals(request.getIsFree());
        boolean hasPrice = request.getPrice() != null;
        if (!hasFree && !hasPrice) {
            throw new AppException(ErrorCode.PRODUCT_PRICE_OR_FREE_REQUIRED);
        }
    }

    /** Upload danh sách ảnh lên Cloudinary và lưu vào DB */
    private void saveImages(Product product, List<MultipartFile> images) {
        String folder = baseFolder + PRODUCT_FOLDER_SUFFIX;
        List<ProductImage> productImages = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            CloudinaryResponse res = cloudinaryService.upload(images.get(i), folder);
            productImages.add(ProductImage.builder()
                    .product(product)
                    .imageUrl(res.getUrl())
                    .imageCloudId(res.getPublicId())
                    .displayOrder(i)
                    .isPrimary(i == 0) // Ảnh đầu tiên trong danh sách gửi lên sẽ là primary
                    .build());
        }
        productImageRepository.saveAll(productImages);
    }

    /** Xóa toàn bộ ảnh của sản phẩm (Cloudinary + DB) */
    private void deleteAllImages(Product product) {
        List<ProductImage> oldImages = productImageRepository
                .findByProduct_ProductIdOrderByDisplayOrderAscCreatedAtAsc(product.getProductId());
        for (ProductImage img : oldImages) {
            if (img.getImageCloudId() != null) {
                try {
                    cloudinaryService.delete(img.getImageCloudId());
                } catch (Exception ex) {
                    log.warn("Không thể xóa ảnh Cloudinary: {}", img.getImageCloudId(), ex);
                }
            }
        }
        productImageRepository.deleteByProduct_ProductId(product.getProductId());
    }

    /** Lưu giá trị thuộc tính động */
    private void saveAttributeValues(Product product, List<ProductAttributeValueRequest> values) {
        if (CollectionUtils.isEmpty(values)) return;
        List<ProductAttributeValue> entities = values.stream().map(v -> {
            ProductAttribute attr = productAttributeRepository.findById(v.getAttributeId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_ATTRIBUTE_NOT_FOUND));
            return ProductAttributeValue.builder()
                    .product(product)
                    .attribute(attr)
                    .value(v.getValue())
                    .build();
        }).collect(Collectors.toList());
        attributeValueRepository.saveAll(entities);
    }

    /** Lưu tags (từ ID có sẵn + tên mới cần tạo) */
    private void saveTags(Product product, List<UUID> tagIds, List<String> newTagNames) {
        List<ProductTag> links = new ArrayList<>();

        if (!CollectionUtils.isEmpty(tagIds)) {
            for (UUID tagId : tagIds) {
                Tag tag = tagRepository.findById(tagId)
                        .orElseThrow(() -> new AppException(ErrorCode.TAG_NOT_FOUND));
                links.add(ProductTag.builder().product(product).tag(tag).build());
                tag.setUsageCount(tag.getUsageCount() + 1);
                tagRepository.save(tag);
            }
        }

        if (!CollectionUtils.isEmpty(newTagNames)) {
            for (String name : newTagNames) {
                Tag tag = tagRepository.findByTagNameIgnoreCase(name).orElseGet(() -> {
                    Tag newTag = Tag.builder()
                            .tagName(name)
                            .slug(SlugUtils.toSlug(name))
                            .usageCount(0)
                            .build();
                    return tagRepository.save(newTag);
                });
                tag.setUsageCount(tag.getUsageCount() + 1);
                tagRepository.save(tag);
                links.add(ProductTag.builder().product(product).tag(tag).build());
            }
        }

        if (!links.isEmpty()) {
            productTagRepository.saveAll(links);
        }
    }

    /** Gỡ toàn bộ tags của sản phẩm và giảm usage_count */
    private void removeTags(Product product) {
        List<ProductTag> oldLinks = productTagRepository.findByProduct_ProductId(product.getProductId());
        if (oldLinks.isEmpty()) return;
        List<UUID> tagIds = oldLinks.stream()
                .map(pt -> pt.getTag().getTagId()).collect(Collectors.toList());
        productTagRepository.deleteByProduct_ProductId(product.getProductId());
        productTagRepository.decrementUsageCountForTags(tagIds);
    }

    /** Resolve University (null nếu không có) */
    private University resolveUniversity(UUID id) {
        if (id == null) return null;
        return universityRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.UNIVERSITY_NOT_FOUND));
    }

    /** Resolve Campus (null nếu không có) */
    private Campus resolveCampus(UUID id) {
        if (id == null) return null;
        return campusRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CAMPUS_NOT_FOUND));
    }

    /** Tạo slug unique từ title */
    private String generateUniqueSlug(String title) {
        String base = SlugUtils.toSlug(title);
        String slug = base;
        int count = 1;
        while (productRepository.existsBySlug(slug)) {
            slug = base + "-" + count++;
        }
        return slug;
    }

    /** Phân trang – parse chuỗi "field,direction" */
    private Pageable buildPageable(int page, int size, String sort) {
        try {
            String[] parts = sort.split(",");
            Sort.Direction dir = parts.length > 1 && "asc".equalsIgnoreCase(parts[1])
                    ? Sort.Direction.ASC : Sort.Direction.DESC;
            return PageRequest.of(page, size, Sort.by(dir, parts[0]));
        } catch (Exception e) {
            return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Mapping
    // ═════════════════════════════════════════════════════════════════════════

    private ProductSummaryResponse toSummary(Product p) {
        // Lấy ảnh chính (is_primary) hoặc ảnh đầu tiên
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
                .isFavorited(isFavorited(p.getProductId()))
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
                .build();
    }

    private ProductResponse toFullResponse(Product p) {
        List<ProductImageResponse> imageResponses = p.getImages().stream()
                .map(img -> ProductImageResponse.builder()
                        .imageId(img.getImageId())
                        .imageUrl(img.getImageUrl())
                        .imageCloudId(img.getImageCloudId())
                        .displayOrder(img.getDisplayOrder())
                        .isPrimary(img.getIsPrimary())
                        .createdAt(img.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        List<ProductAttributeValueResponse> attrResponses = p.getAttributeValues().stream()
                .map(av -> ProductAttributeValueResponse.builder()
                        .attributeId(av.getAttribute().getAttributeId())
                        .attributeName(av.getAttribute().getAttributeName())
                        .attributeType(av.getAttribute().getAttributeType() != null
                                ? av.getAttribute().getAttributeType().name().toLowerCase() : null)
                        .value(av.getValue())
                        .build())
                .collect(Collectors.toList());

        List<TagResponse> tagResponses = p.getProductTags().stream()
                .map(pt -> TagResponse.builder()
                        .tagId(pt.getTag().getTagId())
                        .tagName(pt.getTag().getTagName())
                        .slug(pt.getTag().getSlug())
                        .usageCount(pt.getTag().getUsageCount())
                        .build())
                .collect(Collectors.toList());

        // Thống kê số sản phẩm đã bán của người bán
        Long sellerTotalSales = 0L;
        if (p.getSeller() != null) {
            sellerTotalSales = productRepository.countSoldProductsBySellerId(p.getSeller().getUserId());
        }

        return ProductResponse.builder()
                .productId(p.getProductId())
                .title(p.getTitle())
                .description(p.getDescription())
                .slug(p.getSlug())
                .condition(p.getCondition())
                .price(p.getPrice())
                .isFree(p.getIsFree())
                .isNegotiable(p.getIsNegotiable())
                .listingType(p.getListingType())
                .exchangePreferences(p.getExchangePreferences())
                .transactionType(p.getTransactionType())
                .meetingPoint(p.getMeetingPoint())
                .contactName(p.getContactName())
                .contactPhone(p.getContactPhone())
                .zaloLink(p.getZaloLink())
                .facebookLink(p.getFacebookLink())
                .status(p.getStatus())
                .previousStatus(p.getPreviousStatus())
                .viewCount(p.getViewCount())
                .favoriteCount(p.getFavoriteCount())
                .isFavorited(isFavorited(p.getProductId()))
                .messageCount(p.getMessageCount())
                .expiryDays(p.getExpiryDays())
                .renewalCount(p.getRenewalCount())
                .isFeatured(p.getIsFeatured())
                .expiresAt(p.getExpiresAt())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .approvedAt(p.getApprovedAt())
                .soldAt(p.getSoldAt())
                .categoryId(p.getCategory() != null ? p.getCategory().getCategoryId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getCategoryName() : null)
                .categorySlug(p.getCategory() != null ? p.getCategory().getSlug() : null)
                .universityId(p.getUniversity() != null ? p.getUniversity().getUniversityId() : null)
                .universityName(p.getUniversity() != null ? p.getUniversity().getUniversityName() : null)
                .universityShortName(p.getUniversity() != null ? p.getUniversity().getShortName() : null)
                .campusId(p.getCampus() != null ? p.getCampus().getCampusId() : null)
                .campusName(p.getCampus() != null ? p.getCampus().getCampusName() : null)
                .sellerId(p.getSeller() != null ? p.getSeller().getUserId() : null)
                .sellerName(p.getSeller() != null ? p.getSeller().getFullName() : null)
                .sellerAvatar(p.getSeller() != null ? p.getSeller().getAvatarUrl() : null)
                .sellerReputation(p.getSeller() != null ? p.getSeller().getReputationScore().doubleValue() : null)
                .sellerTotalSales(sellerTotalSales)
                .images(imageResponses)
                .attributeValues(attrResponses)
                .tags(tagResponses)
                .build();
    }

    private Boolean isFavorited(UUID productId) {
        UUID currentUserId = resolveCurrentUserIdOrNull();
        if (currentUserId == null) {
            return false;
        }
        return favoriteRepository.existsByUser_UserIdAndProduct_ProductId(currentUserId, productId);
    }

    private UUID resolveCurrentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String principal = authentication.getName();
        if (principal == null || principal.isBlank() || "anonymousUser".equalsIgnoreCase(principal)) {
            return null;
        }

        try {
            return UUID.fromString(principal);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
