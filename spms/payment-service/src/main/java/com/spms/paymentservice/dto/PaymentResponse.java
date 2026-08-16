package com.spms.paymentservice.dto;

import com.spms.paymentservice.model.entity.Receipt;
import com.spms.paymentservice.model.entity.Transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response returned after a payment is processed successfully.
 */
public record PaymentResponse(
        UUID transactionId,
        String reservationId,
        String status,
        BigDecimal amount,
        String currency,
        String transactionRef,
        Instant processedAt,
        ReceiptSummary receipt
) {
    public record ReceiptSummary(
            String receiptNumber,
            String downloadUrl
    ) {}

    public static PaymentResponse from(Transaction tx) {
        ReceiptSummary receiptSummary = null;
        Receipt receipt = tx.getReceipt();
        if (receipt != null) {
            receiptSummary = new ReceiptSummary(
                    receipt.getReceiptNumber(),
                    "/api/payments/" + tx.getId() + "/receipt"
            );
        }
        return new PaymentResponse(
                tx.getId(),
                tx.getReservationId(),
                tx.getStatus().name(),
                tx.getAmount(),
                tx.getCurrency(),           // String field in entity
                tx.getTransactionRef(),
                tx.getProcessedAt(),        // entity-level processed timestamp
                receiptSummary
        );
    }
}
