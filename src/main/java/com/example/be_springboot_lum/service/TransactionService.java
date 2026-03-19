package com.example.be_springboot_lum.service;

import com.example.be_springboot_lum.dto.request.CreateTransactionRequest;
import com.example.be_springboot_lum.dto.request.UpdateTransactionStatusRequest;
import com.example.be_springboot_lum.dto.response.TransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TransactionService {

    /** Tạo giao dịch mới (buyer → buyer_requested) */
    TransactionResponse createTransaction(UUID buyerId, CreateTransactionRequest request);

    /** Cập nhật trạng thái giao dịch theo luồng đã định nghĩa */
    TransactionResponse updateTransactionStatus(UUID userId, UUID transactionId, UpdateTransactionStatusRequest request);

    /** Danh sách giao dịch của user (cả buyer & seller) */
    Page<TransactionResponse> getMyTransactions(UUID userId, Pageable pageable);

    /** Chi tiết giao dịch kèm lịch sử trạng thái */
    TransactionResponse getTransactionDetail(UUID userId, UUID transactionId);
}
