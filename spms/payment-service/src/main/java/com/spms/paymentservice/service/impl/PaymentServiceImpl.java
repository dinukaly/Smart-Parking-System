package com.spms.paymentservice.service.impl;

import com.spms.paymentservice.dto.PaymentResponse;
import com.spms.paymentservice.dto.ProcessPaymentRequest;
import com.spms.paymentservice.dto.ReceiptResponse;
import com.spms.paymentservice.event.PaymentEventPublisher;
import com.spms.paymentservice.exception.BadRequestException;
import com.spms.paymentservice.exception.ConflictException;
import com.spms.paymentservice.exception.ForbiddenException;
import com.spms.paymentservice.exception.ResourceNotFoundException;
import com.spms.paymentservice.model.entity.Receipt;
import com.spms.paymentservice.model.entity.Transaction;
import com.spms.paymentservice.model.entity.enums.PaymentMethod;
import com.spms.paymentservice.model.entity.enums.TransactionStatus;
import com.spms.paymentservice.repository.ReceiptRepository;
import com.spms.paymentservice.repository.TransactionRepository;
import com.spms.paymentservice.security.SecurityUtils;
import com.spms.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Core payment processing logic for the SPMS Payment Service.
 *
 * All monetary operations use {@code @Transactional} to ensure atomicity.
 * Card validation uses a mock Luhn algorithm — no real payment gateway is integrated.
 */
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    // Simple in-memory sequence for human-readable ref numbers (per restart).
    // In production this would be a DB sequence.
    private final AtomicLong txnSequence   = new AtomicLong(1);
    private final AtomicLong rcpSequence   = new AtomicLong(1);

    private final TransactionRepository transactionRepository;
    private final ReceiptRepository     receiptRepository;
    private final PaymentEventPublisher  eventPublisher;

    // Public API methods

    @Override
    @Transactional
    public PaymentResponse processPayment(ProcessPaymentRequest request, UUID userId) {
        log.info("Processing payment for user={}, reservation={}, amount={}",
                userId, request.reservationId(), request.amount());

        // 1. Prevent duplicate successful payments for the same reservation
        if (transactionRepository.existsByReservationIdAndStatus(
                request.reservationId(), TransactionStatus.SUCCESS)) {
            throw new ConflictException(
                    "A successful payment already exists for reservation: " + request.reservationId());
        }

        // 2. Resolve and validate the payment method
        PaymentMethod method = resolvePaymentMethod(request.paymentMethod());

        // 3. Validate currency code (basic 3-letter check)
        String currency = validateCurrency(request.currency());

        // 4. Perform mock card validation (Luhn algorithm)
        if (method == PaymentMethod.MOCK_CARD) {
            validateCardDetails(request);
        }

        // 5. Build transaction in PENDING state
        String txnRef = generateTransactionRef();
        Transaction transaction = Transaction.builder()
                .userId(userId)
                .reservationId(request.reservationId())
                .amount(request.amount())
                .currency(currency)
                .status(TransactionStatus.PENDING)
                .paymentMethod(method)
                .cardLastFour(extractLastFour(request))
                .transactionRef(txnRef)
                .build();
        transaction = transactionRepository.save(transaction);

        // 6. Simulate payment gateway processing
        boolean paymentApproved = simulateGatewayApproval(request);

        if (paymentApproved) {
            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setProcessedAt(Instant.now());
            transaction = transactionRepository.save(transaction);

            // 7. Issue digital receipt
            Receipt receipt = issueReceipt(transaction);
            transaction.setReceipt(receipt);

            log.info("Payment SUCCESS: txnRef={}, userId={}", txnRef, userId);
        } else {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason("Card declined by mock gateway");
            transaction.setProcessedAt(Instant.now());
            transaction = transactionRepository.save(transaction);

            log.warn("Payment FAILED: txnRef={}, userId={}", txnRef, userId);
        }

        // 8. Publish event to RabbitMQ (best-effort — does not affect response)
        eventPublisher.publishPaymentStatus(transaction);

        return PaymentResponse.from(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getUserPaymentHistory(UUID userId) {
        return transactionRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getTransactionById(UUID transactionId, UUID userId) {
        Transaction transaction = findTransactionOrThrow(transactionId);
        assertOwnerOrAdmin(transaction, userId);
        return PaymentResponse.from(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public ReceiptResponse getReceiptByTransactionId(UUID transactionId, UUID userId) {
        Transaction transaction = findTransactionOrThrow(transactionId);
        assertOwnerOrAdmin(transaction, userId);

        if (transaction.getStatus() != TransactionStatus.SUCCESS) {
            throw new BadRequestException("Receipt is only available for successful transactions.");
        }

        Receipt receipt = receiptRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Receipt not found for transaction: " + transactionId));

        return ReceiptResponse.from(receipt);
    }

    @Override
    @Transactional
    public PaymentResponse refundTransaction(UUID transactionId) {
        Transaction transaction = findTransactionOrThrow(transactionId);

        if (transaction.getStatus() != TransactionStatus.SUCCESS) {
            throw new BadRequestException(
                    "Only successful transactions can be refunded. Current status: " + transaction.getStatus());
        }

        transaction.setStatus(TransactionStatus.REFUNDED);
        transaction.setProcessedAt(Instant.now());
        transaction = transactionRepository.save(transaction);

        log.info("Transaction REFUNDED: txnRef={}", transaction.getTransactionRef());
        eventPublisher.publishPaymentStatus(transaction);

        return PaymentResponse.from(transaction);
    }

    // Private helpers

    private Transaction findTransactionOrThrow(UUID transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Transaction not found: " + transactionId));
    }

    /**
     * Ownership check: the requesting user must own the transaction, or be an ADMIN.
     */
    private void assertOwnerOrAdmin(Transaction transaction, UUID userId) {
        if (!SecurityUtils.isAdmin() && !transaction.getUserId().equals(userId)) {
            throw new ForbiddenException("Access denied to transaction: " + transaction.getId());
        }
    }

    private PaymentMethod resolvePaymentMethod(String raw) {
        try {
            return PaymentMethod.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unsupported payment method: " + raw);
        }
    }

    private String validateCurrency(String currency) {
        if (currency == null || currency.length() != 3) {
            throw new BadRequestException("Currency must be a 3-letter ISO 4217 code (e.g. USD).");
        }
        return currency.toUpperCase();
    }

    /**
     * Validates mock card details — runs the Luhn algorithm on the card number
     * and performs basic expiry/CVV format checks.
     */
    private void validateCardDetails(ProcessPaymentRequest request) {
        if (request.cardDetails() == null) {
            throw new BadRequestException("Card details are required for MOCK_CARD payment method.");
        }

        String cardNumber = request.cardDetails().cardNumber().replaceAll("\\s+", "");

        if (!luhnCheck(cardNumber)) {
            throw new BadRequestException("Invalid card number (Luhn check failed).");
        }

        // Basic expiry format validation
        try {
            int month = Integer.parseInt(request.cardDetails().expiryMonth());
            int year  = Integer.parseInt(request.cardDetails().expiryYear());
            int currentYear = java.time.Year.now(ZoneOffset.UTC).getValue();
            int currentMonth = java.time.MonthDay.now(ZoneOffset.UTC).getMonthValue();

            if (month < 1 || month > 12) {
                throw new BadRequestException("Invalid expiry month.");
            }
            if (year < currentYear || (year == currentYear && month < currentMonth)) {
                throw new BadRequestException("Card has expired.");
            }
        } catch (NumberFormatException e) {
            throw new BadRequestException("Invalid expiry date format.");
        }

        // CVV: 3 or 4 digits
        String cvv = request.cardDetails().cvv();
        if (!cvv.matches("\\d{3,4}")) {
            throw new BadRequestException("CVV must be 3 or 4 digits.");
        }
    }

    /**
     * Luhn algorithm for card number validation.
     * Returns true if the card number passes the check.
     */
    private boolean luhnCheck(String number) {
        if (number == null || !number.matches("\\d+")) return false;
        int sum = 0;
        boolean alternate = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int n = Character.getNumericValue(number.charAt(i));
            if (alternate) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }

    /**
     * Simulates a payment gateway. Luhn-valid cards are always approved
     * in the mock environment (real gateway integration would go here).
     */
    private boolean simulateGatewayApproval(ProcessPaymentRequest request) {
        // For MOCK_CARD: any Luhn-valid card passes (already validated above)
        return true;
    }

    private String extractLastFour(ProcessPaymentRequest request) {
        if (request.cardDetails() == null) return null;
        String number = request.cardDetails().cardNumber().replaceAll("\\s+", "");
        return number.length() >= 4
                ? number.substring(number.length() - 4)
                : number;
    }

    private String generateTransactionRef() {
        String date = DATE_FMT.format(Instant.now());
        long seq = txnSequence.getAndIncrement();
        return String.format("TXN-%s-%03d", date, seq);
    }

    private String generateReceiptNumber() {
        String date = DATE_FMT.format(Instant.now());
        long seq = rcpSequence.getAndIncrement();
        return String.format("RCP-%s-%03d", date, seq);
    }

    private Receipt issueReceipt(Transaction transaction) {
        Receipt receipt = Receipt.builder()
                .transaction(transaction)
                .receiptNumber(generateReceiptNumber())
                .issuedAt(Instant.now())
                .details("Payment receipt for reservation: " + transaction.getReservationId())
                .build();
        return receiptRepository.save(receipt);
    }
}
