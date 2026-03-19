package com.example.be_springboot_lum.service.impl;

import com.example.be_springboot_lum.dto.request.CreateTransactionRequest;
import com.example.be_springboot_lum.dto.request.UpdateTransactionStatusRequest;
import com.example.be_springboot_lum.dto.response.TransactionResponse;
import com.example.be_springboot_lum.dto.response.TransactionStatusHistoryResponse;
import com.example.be_springboot_lum.exception.AppException;
import com.example.be_springboot_lum.exception.ErrorCode;
import com.example.be_springboot_lum.model.Conversation;
import com.example.be_springboot_lum.model.Message;
import com.example.be_springboot_lum.model.Product;
import com.example.be_springboot_lum.model.Transaction;
import com.example.be_springboot_lum.model.TransactionStatusHistory;
import com.example.be_springboot_lum.model.User;
import com.example.be_springboot_lum.repository.ConversationParticipantRepository;
import com.example.be_springboot_lum.repository.ConversationRepository;
import com.example.be_springboot_lum.repository.MessageRepository;
import com.example.be_springboot_lum.repository.ProductRepository;
import com.example.be_springboot_lum.repository.ReviewRepository;
import com.example.be_springboot_lum.repository.TransactionRepository;
import com.example.be_springboot_lum.repository.TransactionStatusHistoryRepository;
import com.example.be_springboot_lum.repository.UserRepository;
import com.example.be_springboot_lum.service.NotificationService;
import com.example.be_springboot_lum.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionStatusHistoryRepository statusHistoryRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    // ─── Trạng thái cho phép chuyển ──────────────────────────────────────────
    // Mapping: trạng thái hiện tại → tập trạng thái được phép chuyển sang
    private static final java.util.Map<String, Set<String>> VALID_TRANSITIONS = new java.util.HashMap<>();

    static {
        VALID_TRANSITIONS.put("buyer_requested", Set.of("seller_confirmed", "cancelled"));
        // seller_confirmed -> seller_confirmed: cho phép seller cập nhật meetupLocation/meetupTime
        VALID_TRANSITIONS.put("seller_confirmed", Set.of("seller_confirmed", "meetup_confirmed", "cancelled", "disputed"));
        VALID_TRANSITIONS.put("meetup_confirmed", Set.of("payment_pending", "completed", "cancelled", "disputed"));
        VALID_TRANSITIONS.put("payment_pending", Set.of("completed", "cancelled", "disputed"));
        VALID_TRANSITIONS.put("completed", Set.of());
        VALID_TRANSITIONS.put("cancelled", Set.of());
        VALID_TRANSITIONS.put("disputed", Set.of("completed", "cancelled"));
    }

    // ─── createTransaction ────────────────────────────────────────────────────

    @Override
    @Transactional
    public TransactionResponse createTransaction(UUID buyerId, CreateTransactionRequest request) {
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        // Không thể mua sản phẩm của chính mình
        if (product.getSeller().getUserId().equals(buyerId)) {
            throw new AppException(ErrorCode.TRANSACTION_FORBIDDEN);
        }

        // Sản phẩm phải ở trạng thái available
        if (!"available".equals(product.getStatus())) {
            throw new AppException(ErrorCode.TRANSACTION_PRODUCT_NOT_AVAILABLE);
        }

        // Kiểm tra giao dịch đang diễn ra
        transactionRepository.findActiveByBuyerAndProduct(buyerId, product.getProductId())
                .ifPresent(t -> {
                    throw new AppException(ErrorCode.TRANSACTION_ALREADY_EXISTS);
                });

        User seller = product.getSeller();

        Transaction transaction = Transaction.builder()
                .product(product)
                .buyer(buyer)
                .seller(seller)
                .transactionType(request.getTransactionType() != null ? request.getTransactionType() : "sale")
                .status("buyer_requested")
                .agreedPrice(request.getAgreedPrice() != null ? request.getAgreedPrice() : product.getPrice())
                .paymentMethod(request.getPaymentMethod())
                .shippingMethod(request.getShippingMethod() != null ? request.getShippingMethod()
                        : product.getTransactionType())
                .meetupLocation(request.getMeetupLocation())
                .meetupTime(request.getMeetupTime())
                .notes(request.getNotes())
                .requestedAt(OffsetDateTime.now())
                .build();

        transaction = transactionRepository.save(transaction);

        // Ghi lịch sử trạng thái
        saveStatusHistory(transaction, "buyer_requested", buyer, "Giao dịch được tạo bởi người mua");

        // Cập nhật trạng thái sản phẩm → pending
        product.setPreviousStatus(product.getStatus());
        product.setStatus("pending");
        productRepository.save(product);

        // Liên kết conversation (nếu đã có giữa buyer-seller cho sản phẩm này)
        linkTransactionToConversation(transaction, buyerId, seller.getUserId(), product.getProductId());

        // Gửi transaction_event message vào conversation
        sendTransactionEventMessage(transaction, "buyer_requested");

        // Gửi thông báo cho seller
        notificationService.sendNotification(
                seller.getUserId(),
                "transaction_update",
                "Có người muốn mua sản phẩm của bạn",
                buyer.getFullName() + " muốn mua \"" + product.getTitle() + "\"",
                buyerId,
                "transaction",
                transaction.getTransactionId(),
                "/tai-khoan/lich-su-giao-dich");

        return mapToResponse(transaction, fetchStatusHistory(transaction.getTransactionId()));
    }

    // ─── updateTransactionStatus ──────────────────────────────────────────────

    @Override
    @Transactional
    public TransactionResponse updateTransactionStatus(UUID userId, UUID transactionId,
            UpdateTransactionStatusRequest request) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));

        boolean isBuyer = transaction.getBuyer().getUserId().equals(userId);
        boolean isSeller = transaction.getSeller().getUserId().equals(userId);

        if (!isBuyer && !isSeller) {
            throw new AppException(ErrorCode.TRANSACTION_FORBIDDEN);
        }

        String currentStatus = transaction.getStatus();
        String newStatus = request.getStatus();

        // Kiểm tra transition hợp lệ
        Set<String> allowed = VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowed.contains(newStatus)) {
            throw new AppException(ErrorCode.TRANSACTION_INVALID_STATUS);
        }

        User actor = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // ── Quyền theo vai trò ────────────────────────────────────────────────
        switch (newStatus) {
            case "seller_confirmed" -> {
                if (!isSeller)
                    throw new AppException(ErrorCode.TRANSACTION_FORBIDDEN);
                transaction.setSellerConfirmedAt(OffsetDateTime.now());
                if (request.getMeetupLocation() != null) {
                    transaction.setMeetupLocation(request.getMeetupLocation());
                    // Người bán đã đưa ra địa điểm thì nghiễm nhiên là họ đã xác nhận địa điểm đó
                    transaction.setSellerConfirmedMeetup(true);
                }
                if (request.getMeetupTime() != null)
                    transaction.setMeetupTime(request.getMeetupTime());
                if (request.getAgreedPrice() != null)
                    transaction.setAgreedPrice(request.getAgreedPrice());
            }
            case "meetup_confirmed" -> {
                // Cả hai cần xác nhận
                if (isBuyer) {
                    transaction.setBuyerConfirmedMeetup(true);
                    if (request.getPaymentMethod() != null) {
                        transaction.setPaymentMethod(request.getPaymentMethod());
                    }
                }
                if (isSeller)
                    transaction.setSellerConfirmedMeetup(true);
                // Nếu người bán đã gửi đề xuất địa điểm thì tự động xem như họ đã xác nhận
                if (transaction.getMeetupLocation() != null) {
                    transaction.setSellerConfirmedMeetup(true);
                }
                // Chỉ thực sự chuyển sang meetup_confirmed khi cả hai đồng ý
                if (!Boolean.TRUE.equals(transaction.getBuyerConfirmedMeetup())
                        || !Boolean.TRUE.equals(transaction.getSellerConfirmedMeetup())) {
                    // Chưa đủ 2 bên → chỉ cập nhật flag, không đổi status
                    transactionRepository.save(transaction);
                    saveStatusHistory(transaction, currentStatus, actor,
                            (isBuyer ? "Người mua" : "Người bán") + " đã xác nhận lịch gặp");
                    // Fix realtime: Gửi webSocket update ngay cả khi status chưa đổi để bên kia thấy flag thay đổi
                    sendTransactionEventMessage(transaction, currentStatus);
                    return mapToResponse(transaction, fetchStatusHistory(transactionId));
                }
                transaction.setMeetupConfirmedAt(OffsetDateTime.now());
            }
            case "payment_pending" -> {
                // Seller chuyển sang payment_pending sau khi hàng đã trao
                if (!isSeller)
                    throw new AppException(ErrorCode.TRANSACTION_FORBIDDEN);
            }
            case "completed" -> {
                // Cả hai xác nhận thanh toán
                if (isBuyer)
                    transaction.setBuyerConfirmedPayment(true);
                if (isSeller)
                    transaction.setSellerConfirmedPayment(true);
                if (!Boolean.TRUE.equals(transaction.getBuyerConfirmedPayment())
                        || !Boolean.TRUE.equals(transaction.getSellerConfirmedPayment())) {
                    transactionRepository.save(transaction);
                    saveStatusHistory(transaction, currentStatus, actor,
                            (isBuyer ? "Người mua" : "Người bán") + " đã xác nhận thanh toán");
                    // Fix realtime: Gửi webSocket update ngay lập tức để người còn lại thấy tick xanh (đã xác nhận)
                    sendTransactionEventMessage(transaction, currentStatus);
                    return mapToResponse(transaction, fetchStatusHistory(transactionId));
                }
                transaction.setCompletedAt(OffsetDateTime.now());
                completeTransaction(transaction);
            }
            case "cancelled" -> {
                if (request.getCancellationReason() == null || request.getCancellationReason().isBlank()) {
                    throw new AppException(ErrorCode.TRANSACTION_INVALID_STATUS);
                }
                transaction.setCancellationReason(request.getCancellationReason());
                transaction.setCancelledBy(userId);
                transaction.setCancelledAt(OffsetDateTime.now());
                cancelTransaction(transaction);
            }
            case "disputed" -> {
                // Bất kỳ bên nào cũng có thể mở tranh chấp
            }
        }

        transaction.setStatus(newStatus);
        if (request.getNotes() != null)
            transaction.setNotes(request.getNotes());
        transaction = transactionRepository.save(transaction);

        saveStatusHistory(transaction, newStatus, actor, request.getNotes());

        // Gửi transaction_event vào conversation
        sendTransactionEventMessage(transaction, newStatus);

        // Gửi thông báo cho bên kia
        UUID otherUserId = isBuyer ? transaction.getSeller().getUserId() : transaction.getBuyer().getUserId();
        String notifTitle = buildNotifTitle(newStatus);
        String notifContent = buildNotifContent(newStatus, transaction, isBuyer ? "Người mua" : "Người bán");

        notificationService.sendNotification(
                otherUserId,
                "transaction_update",
                notifTitle,
                notifContent,
                userId,
                "transaction",
                transaction.getTransactionId(),
                "/tai-khoan/lich-su-giao-dich");

        return mapToResponse(transaction, fetchStatusHistory(transactionId));
    }

    // ─── getMyTransactions ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getMyTransactions(UUID userId, Pageable pageable) {
        return transactionRepository.findByUserId(userId, pageable)
                .map(t -> mapToResponse(t, null)); // không load history cho list view
    }

    // ─── getTransactionDetail ─────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionDetail(UUID userId, UUID transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));

        boolean isBuyer = transaction.getBuyer().getUserId().equals(userId);
        boolean isSeller = transaction.getSeller().getUserId().equals(userId);
        if (!isBuyer && !isSeller) {
            throw new AppException(ErrorCode.TRANSACTION_FORBIDDEN);
        }

        return mapToResponse(transaction, fetchStatusHistory(transactionId));
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    /** Liên kết conversation (nếu đã tồn tại) với transaction mới tạo */
    private void linkTransactionToConversation(Transaction transaction, UUID buyerId, UUID sellerId, UUID productId) {
        try {
            // Tìm conversation giữa buyer-seller cho sản phẩm này
            var participants = participantRepository.findByUserUserId(buyerId);
            for (var myPart : participants) {
                Conversation conv = myPart.getConversation();
                if (conv.getProduct() != null && conv.getProduct().getProductId().equals(productId)) {
                    var others = participantRepository.findByConversationConversationId(conv.getConversationId());
                    boolean hasOther = others.stream().anyMatch(p -> p.getUser().getUserId().equals(sellerId));
                    if (hasOther) {
                        conv.setTransactionId(transaction.getTransactionId());
                        conversationRepository.save(conv);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not link conversation to transaction {}: {}", transaction.getTransactionId(),
                    e.getMessage());
        }
    }

    /** Gửi message loại transaction_event vào conversation liên quan */
    private void sendTransactionEventMessage(Transaction transaction, String eventType) {
        try {
            // Tìm conversation đã liên kết với transaction này
            java.util.Optional<Conversation> convOpt = conversationRepository
                    .findFirstByTransactionId(transaction.getTransactionId());

            if (convOpt.isEmpty())
                return;

            Conversation conv = convOpt.get();
            Message eventMsg = Message.builder()
                    .conversation(conv)
                    .sender(transaction.getBuyer()) // system message dùng buyer làm sender
                    .messageType("transaction_event")
                    .transactionEventType(eventType)
                    .content(buildEventContent(eventType, transaction))
                    .deliveryStatus("sent")
                    .build();

            eventMsg = messageRepository.save(eventMsg);

            // Push qua WebSocket tới tất cả participants
            java.util.Map<String, Object> msgResponse = buildEventMessageResponse(eventMsg, transaction);
            var participants = participantRepository.findByConversationConversationId(conv.getConversationId());
            for (var p : participants) {
                messagingTemplate.convertAndSend(
                        "/topic/user-" + p.getUser().getUserId(),
                        (Object) msgResponse);
            }
        } catch (Exception e) {
            log.warn("Could not send transaction event message for {}: {}", transaction.getTransactionId(),
                    e.getMessage());
        }
    }

    /** Xử lý hoàn thành giao dịch: cập nhật sản phẩm + user stats */
    private void completeTransaction(Transaction transaction) {
        Product product = transaction.getProduct();
        product.setStatus("sold");
        product.setSoldAt(OffsetDateTime.now());
        productRepository.save(product);

        // Tăng thống kê
        User seller = transaction.getSeller();
        seller.setTotalSales(seller.getTotalSales() + 1);
        userRepository.save(seller);

        User buyer = transaction.getBuyer();
        buyer.setTotalPurchases(buyer.getTotalPurchases() + 1);
        userRepository.save(buyer);

        // Thông báo product_sold cho seller
        notificationService.sendNotification(
                seller.getUserId(),
                "product_sold",
                "Sản phẩm đã được bán thành công!",
                "\"" + product.getTitle() + "\" đã hoàn thiện giao dịch.",
                buyer.getUserId(),
                "product",
                product.getProductId(),
                "/san-pham/" + product.getSlug());
    }

    /** Khôi phục trạng thái sản phẩm khi huỷ giao dịch */
    private void cancelTransaction(Transaction transaction) {
        Product product = transaction.getProduct();
        String prevStatus = product.getPreviousStatus();
        product.setStatus(prevStatus != null ? prevStatus : "available");
        product.setPreviousStatus(null);
        productRepository.save(product);
    }

    /** Lưu một bản ghi vào lịch sử trạng thái */
    private void saveStatusHistory(Transaction transaction, String status, User changedBy, String notes) {
        TransactionStatusHistory history = TransactionStatusHistory.builder()
                .transaction(transaction)
                .status(status)
                .changedBy(changedBy)
                .notes(notes)
                .build();
        statusHistoryRepository.save(history);
    }

    private List<TransactionStatusHistoryResponse> fetchStatusHistory(UUID transactionId) {
        return statusHistoryRepository
                .findByTransactionTransactionIdOrderByCreatedAtAsc(transactionId)
                .stream()
                .map(h -> TransactionStatusHistoryResponse.builder()
                        .historyId(h.getHistoryId())
                        .status(h.getStatus())
                        .changedById(h.getChangedBy() != null ? h.getChangedBy().getUserId() : null)
                        .changedByName(h.getChangedBy() != null ? h.getChangedBy().getFullName() : "Hệ thống")
                        .notes(h.getNotes())
                        .createdAt(h.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /** Map Transaction entity → DTO */
    private TransactionResponse mapToResponse(Transaction t, List<TransactionStatusHistoryResponse> history) {
        Product product = t.getProduct();
        User buyer = t.getBuyer();
        User seller = t.getSeller();

        // Lấy ảnh đầu tiên của sản phẩm
        String productImageUrl = (product.getImages() != null && !product.getImages().isEmpty())
                ? product.getImages().get(0).getImageUrl()
                : null;

        boolean isReviewed = reviewRepository.existsByTransaction_TransactionId(t.getTransactionId());

        return TransactionResponse.builder()
                .transactionId(t.getTransactionId())
                // Product
                .productId(product.getProductId())
                .productTitle(product.getTitle())
                .productSlug(product.getSlug())
                .productImageUrl(productImageUrl)
                .productPrice(product.getPrice())
                // Buyer
                .buyerId(buyer.getUserId())
                .buyerName(buyer.getFullName())
                .buyerAvatarUrl(buyer.getAvatarUrl())
                // Seller
                .sellerId(seller.getUserId())
                .sellerName(seller.getFullName())
                .sellerAvatarUrl(seller.getAvatarUrl())
                // Info
                .transactionType(t.getTransactionType())
                .status(t.getStatus())
                .agreedPrice(t.getAgreedPrice())
                .paymentMethod(t.getPaymentMethod())
                .shippingMethod(t.getShippingMethod())
                .meetupLocation(t.getMeetupLocation())
                .meetupTime(t.getMeetupTime())
                // Confirmations
                .buyerConfirmedMeetup(t.getBuyerConfirmedMeetup())
                .sellerConfirmedMeetup(t.getSellerConfirmedMeetup())
                .buyerConfirmedPayment(t.getBuyerConfirmedPayment())
                .sellerConfirmedPayment(t.getSellerConfirmedPayment())
                // Review
                .isReviewed(isReviewed)
                // Cancel / dispute
                .cancellationReason(t.getCancellationReason())
                .cancelledBy(t.getCancelledBy())
                .notes(t.getNotes())
                // Timeline
                .requestedAt(t.getRequestedAt())
                .sellerConfirmedAt(t.getSellerConfirmedAt())
                .meetupConfirmedAt(t.getMeetupConfirmedAt())
                .completedAt(t.getCompletedAt())
                .cancelledAt(t.getCancelledAt())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                // History
                .statusHistory(history)
                .build();
    }

    // ─── Message / Notification content builders ──────────────────────────────

    private String buildEventContent(String eventType, Transaction transaction) {
        return switch (eventType) {
            case "buyer_requested" -> "🛒 Người mua đã gửi yêu cầu mua hàng";
            case "seller_confirmed" -> "✅ Người bán đã xác nhận giao dịch";
            case "meetup_confirmed" -> "📍 Cả hai đã xác nhận lịch gặp mặt";
            case "payment_pending" -> "💳 Đang chờ xác nhận thanh toán";
            case "completed" -> "🎉 Giao dịch đã hoàn tất thành công!";
            case "cancelled" -> "❌ Giao dịch đã bị huỷ. Lý do: " + transaction.getCancellationReason();
            case "disputed" -> "⚠️ Giao dịch đang trong tình trạng tranh chấp";
            default -> "Cập nhật trạng thái: " + eventType;
        };
    }

    private String buildNotifTitle(String newStatus) {
        return switch (newStatus) {
            case "seller_confirmed" -> "Người bán đã xác nhận giao dịch";
            case "meetup_confirmed" -> "Đã xác nhận lịch gặp mặt";
            case "payment_pending" -> "Đang chờ xác nhận thanh toán";
            case "completed" -> "Giao dịch hoàn tất thành công!";
            case "cancelled" -> "Giao dịch đã bị huỷ";
            case "disputed" -> "Giao dịch đang bị tranh chấp";
            default -> "Cập nhật giao dịch";
        };
    }

    private String buildNotifContent(String newStatus, Transaction t, String actor) {
        return switch (newStatus) {
            case "seller_confirmed" -> actor + " đã xác nhận muốn bán \"" + t.getProduct().getTitle() + "\"";
            case "meetup_confirmed" -> actor + " đã xác nhận lịch gặp";
            case "payment_pending" -> actor + " cho biết hàng đã được trao, chờ bạn xác nhận thanh toán";
            case "completed" -> "Giao dịch sản phẩm \"" + t.getProduct().getTitle() + "\" đã hoàn tất";
            case "cancelled" -> actor + " đã huỷ giao dịch: " + t.getCancellationReason();
            case "disputed" -> actor + " đã mở tranh chấp cho giao dịch";
            default -> "Trạng thái giao dịch được cập nhật";
        };
    }

    /** Tạo map đơn giản chứa thông tin event message để push WebSocket */
    private java.util.Map<String, Object> buildEventMessageResponse(Message msg, Transaction transaction) {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("messageId", msg.getMessageId());
        map.put("conversationId", msg.getConversation().getConversationId());
        map.put("senderId", msg.getSender().getUserId());
        map.put("senderName", msg.getSender().getFullName());
        map.put("messageType", msg.getMessageType());
        map.put("transactionEventType", msg.getTransactionEventType());
        map.put("content", msg.getContent());
        map.put("deliveryStatus", msg.getDeliveryStatus());
        map.put("createdAt", msg.getCreatedAt());
        // ── Transaction detail fields (for real-time UI sync on receiver side) ──
        if (transaction != null) {
            map.put("transactionId", transaction.getTransactionId());
            map.put("transactionStatus", transaction.getStatus());
            map.put("meetupLocation", transaction.getMeetupLocation());
            map.put("meetupTime", transaction.getMeetupTime() != null ? transaction.getMeetupTime().toString() : null);
            map.put("agreedPrice", transaction.getAgreedPrice());
            map.put("buyerConfirmedMeetup", transaction.getBuyerConfirmedMeetup());
            map.put("sellerConfirmedMeetup", transaction.getSellerConfirmedMeetup());
            map.put("buyerConfirmedPayment", transaction.getBuyerConfirmedPayment());
            map.put("sellerConfirmedPayment", transaction.getSellerConfirmedPayment());
        }
        return map;
    }
}
