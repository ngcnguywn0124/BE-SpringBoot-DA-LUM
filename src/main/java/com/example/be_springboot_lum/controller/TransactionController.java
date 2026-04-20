package com.example.be_springboot_lum.controller;

import com.example.be_springboot_lum.dto.request.CreateTransactionRequest;
import com.example.be_springboot_lum.dto.request.UpdateTransactionStatusRequest;
import com.example.be_springboot_lum.dto.response.TransactionResponse;
import com.example.be_springboot_lum.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST API cho Giao dịch (Transaction).
 *
 * NOTE: Hiện dùng @RequestHeader("User-Id") để xác định user như các controller khác trong dự án.
 * Thay bằng @AuthenticationPrincipal khi tích hợp Spring Security đầy đủ.
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * POST /api/transactions
     * Tạo giao dịch mới (buyer → buyer_requested).
     */
    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @RequestHeader("User-Id") UUID currentUserId,
            @RequestBody CreateTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.createTransaction(currentUserId, request));
    }

    /**
     * GET /api/transactions
     * Lấy danh sách giao dịch của user (cả vai trò buyer & seller).
     */
    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> getMyTransactions(
            @RequestHeader("User-Id") UUID currentUserId,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            Pageable pageable) {
        return ResponseEntity.ok(transactionService.getMyTransactions(
                currentUserId,
                pageable,
                role,
                status,
                fromDate,
                toDate));
    }

    /**
     * GET /api/transactions/{transactionId}
     * Chi tiết giao dịch kèm lịch sử trạng thái.
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransactionDetail(
            @RequestHeader("User-Id") UUID currentUserId,
            @PathVariable UUID transactionId) {
        return ResponseEntity.ok(transactionService.getTransactionDetail(currentUserId, transactionId));
    }

    /**
     * PUT /api/transactions/{transactionId}/status
     * Cập nhật trạng thái giao dịch theo luồng đã định nghĩa.
     */
    @PutMapping("/{transactionId}/status")
    public ResponseEntity<TransactionResponse> updateTransactionStatus(
            @RequestHeader("User-Id") UUID currentUserId,
            @PathVariable UUID transactionId,
            @RequestBody UpdateTransactionStatusRequest request) {
        return ResponseEntity.ok(
                transactionService.updateTransactionStatus(currentUserId, transactionId, request));
    }
}
