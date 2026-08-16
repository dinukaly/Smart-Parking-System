package com.spms.paymentservice.dto;

import com.spms.paymentservice.model.entity.Receipt;
import com.spms.paymentservice.model.entity.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Full receipt details returned from GET /api/payments/{id}/receipt.
 */
public record ReceiptResponse(
        UUID receiptId,
        String receiptNumber,
        UUID transactionId,
        String transactionRef,
        String reservationId,
        UUID userId,
        BigDecimal amount,
        String currency,
        String paymentMethod,
        Instant issuedAt
) {
    public static ReceiptResponse from(Receipt r) {
        Transaction tx = r.getTransaction();
        return new ReceiptResponse(
                r.getId(),
                r.getReceiptNumber(),
                tx.getId(),
                tx.getTransactionRef(),
                tx.getReservationId(),
                tx.getUserId(),
                tx.getAmount(),
                tx.getCurrency(),               // String field in entity
                tx.getPaymentMethod().name(),   // enum field
                r.getIssuedAt()
        );
    }
}
