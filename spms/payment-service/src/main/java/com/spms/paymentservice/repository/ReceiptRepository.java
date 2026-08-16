package com.spms.paymentservice.repository;

import com.spms.paymentservice.model.entity.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {

    /** Lookup receipt by the parent transaction ID. */
    Optional<Receipt> findByTransactionId(UUID transactionId);
}
