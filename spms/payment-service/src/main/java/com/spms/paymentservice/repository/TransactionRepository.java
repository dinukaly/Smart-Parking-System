package com.spms.paymentservice.repository;

import com.spms.paymentservice.model.entity.Transaction;
import com.spms.paymentservice.model.entity.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /** Fetch all transactions for a specific user, ordered newest-first. */
    List<Transaction> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /** Check if a reservation already has a successful payment (prevent duplicates). */
    Optional<Transaction> findByReservationIdAndStatus(String reservationId, TransactionStatus status);

    /** Exists check for any transaction on a reservation. */
    boolean existsByReservationIdAndStatus(String reservationId, TransactionStatus status);
}
