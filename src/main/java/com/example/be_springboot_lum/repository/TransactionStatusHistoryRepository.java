package com.example.be_springboot_lum.repository;

import com.example.be_springboot_lum.model.TransactionStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransactionStatusHistoryRepository extends JpaRepository<TransactionStatusHistory, UUID> {

    List<TransactionStatusHistory> findByTransactionTransactionIdOrderByCreatedAtAsc(UUID transactionId);
}
