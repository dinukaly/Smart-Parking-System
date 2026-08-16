package com.spms.paymentservice.service;

import com.spms.paymentservice.dto.PaymentResponse;
import com.spms.paymentservice.dto.ProcessPaymentRequest;
import com.spms.paymentservice.dto.ReceiptResponse;

import java.util.List;
import java.util.UUID;

/**
 * Contract for Payment Service business logic.
 */
public interface PaymentService {

    /**
     * Process a new payment for a parking reservation.
     *
     * @param request the payment request payload
     * @param userId  the authenticated user's UUID
     * @return the resulting transaction and receipt summary
     */
    PaymentResponse processPayment(ProcessPaymentRequest request, UUID userId);

    /**
     * Retrieve all transactions for the authenticated user.
     *
     * @param userId the authenticated user's UUID
     * @return list of payment responses
     */
    List<PaymentResponse> getUserPaymentHistory(UUID userId);

    /**
     * Retrieve a single transaction by its ID.
     * Users can only see their own transactions; ADMIN can see any.
     *
     * @param transactionId the transaction UUID
     * @param userId        the requesting user's UUID
     * @return payment response
     */
    PaymentResponse getTransactionById(UUID transactionId, UUID userId);

    /**
     * Retrieve the digital receipt for a successful transaction.
     *
     * @param transactionId the transaction UUID
     * @param userId        the requesting user's UUID
     * @return receipt response
     */
    ReceiptResponse getReceiptByTransactionId(UUID transactionId, UUID userId);

    /**
     * Refund a successful transaction. ADMIN only.
     *
     * @param transactionId the transaction UUID to refund
     * @return updated payment response with REFUNDED status
     */
    PaymentResponse refundTransaction(UUID transactionId);
}
