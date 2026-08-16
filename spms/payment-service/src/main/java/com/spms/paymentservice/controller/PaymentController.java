package com.spms.paymentservice.controller;

import com.spms.paymentservice.dto.PaymentResponse;
import com.spms.paymentservice.dto.ProcessPaymentRequest;
import com.spms.paymentservice.dto.ReceiptResponse;
import com.spms.paymentservice.security.SecurityUtils;
import com.spms.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for payment operations.
 * All endpoints require a valid JWT Bearer token (enforced by SecurityConfig).
 * The refund endpoint additionally requires ROLE_ADMIN (enforced by @PreAuthorize).
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * POST /api/payments/process
     * Process a payment for a parking reservation.
     */
    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(
            @Valid @RequestBody ProcessPaymentRequest request
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        PaymentResponse response = paymentService.processPayment(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/payments
     * List all payments for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getUserPaymentHistory() {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<PaymentResponse> history = paymentService.getUserPaymentHistory(userId);
        return ResponseEntity.ok(history);
    }

    /**
     * GET /api/payments/{id}
     * Get a specific transaction. Users can only view their own; ADMINs can view any.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getTransaction(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        PaymentResponse response = paymentService.getTransactionById(id, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/payments/{id}/receipt
     * Download the digital receipt for a successful transaction.
     */
    @GetMapping("/{id}/receipt")
    public ResponseEntity<ReceiptResponse> getReceipt(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ReceiptResponse receipt = paymentService.getReceiptByTransactionId(id, userId);
        return ResponseEntity.ok(receipt);
    }

    /**
     * POST /api/payments/{id}/refund
     * Refund a successful transaction. ADMIN only.
     */
    @PostMapping("/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentResponse> refundTransaction(@PathVariable UUID id) {
        PaymentResponse response = paymentService.refundTransaction(id);
        return ResponseEntity.ok(response);
    }
}
