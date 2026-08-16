package com.spms.paymentservice.service;

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
import com.spms.paymentservice.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private PaymentEventPublisher eventPublisher;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private UUID userId;
    private ProcessPaymentRequest validRequest;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        validRequest = new ProcessPaymentRequest(
                "res-12345",
                new BigDecimal("15.50"),
                "USD",
                "MOCK_CARD",
                new ProcessPaymentRequest.CardDetails(
                        "4111111111111111", // Valid Visa mock card
                        "12",
                        "2029",
                        "123",
                        "John Doe"
                )
        );
    }

    @Test
    @DisplayName("Should successfully process valid payment and issue receipt")
    void testProcessPayment_Success() {
        when(transactionRepository.existsByReservationIdAndStatus("res-12345", TransactionStatus.SUCCESS))
                .thenReturn(false);

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction tx = invocation.getArgument(0);
            if (tx.getId() == null) {
                tx.setId(UUID.randomUUID());
            }
            return tx;
        });

        when(receiptRepository.save(any(Receipt.class))).thenAnswer(invocation -> {
            Receipt r = invocation.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        PaymentResponse response = paymentService.processPayment(validRequest, userId);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.amount()).isEqualByComparingTo("15.50");
        assertThat(response.currency()).isEqualTo("USD");
        assertThat(response.receipt()).isNotNull();
        assertThat(response.receipt().receiptNumber()).startsWith("RCP-");

        verify(eventPublisher, times(1)).publishPaymentStatus(any(Transaction.class));
    }

    @Test
    @DisplayName("Should throw ConflictException when reservation is already successfully paid")
    void testProcessPayment_DuplicatePayment_Conflict() {
        when(transactionRepository.existsByReservationIdAndStatus("res-12345", TransactionStatus.SUCCESS))
                .thenReturn(true);

        assertThatThrownBy(() -> paymentService.processPayment(validRequest, userId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("A successful payment already exists for reservation");

        verify(transactionRepository, never()).save(any());
        verify(receiptRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw BadRequestException when card number fails Luhn algorithm")
    void testProcessPayment_InvalidLuhnCard() {
        ProcessPaymentRequest invalidCardRequest = new ProcessPaymentRequest(
                "res-12345",
                new BigDecimal("10.00"),
                "USD",
                "MOCK_CARD",
                new ProcessPaymentRequest.CardDetails(
                        "4111111111111112", // Invalid Luhn checksum
                        "12",
                        "2029",
                        "123",
                        "John Doe"
                )
        );

        when(transactionRepository.existsByReservationIdAndStatus("res-12345", TransactionStatus.SUCCESS))
                .thenReturn(false);

        assertThatThrownBy(() -> paymentService.processPayment(invalidCardRequest, userId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Luhn check failed");
    }

    @Test
    @DisplayName("Should throw BadRequestException when card is expired")
    void testProcessPayment_ExpiredCard() {
        ProcessPaymentRequest expiredCardRequest = new ProcessPaymentRequest(
                "res-12345",
                new BigDecimal("10.00"),
                "USD",
                "MOCK_CARD",
                new ProcessPaymentRequest.CardDetails(
                        "4111111111111111",
                        "01",
                        "2020", // Past year
                        "123",
                        "John Doe"
                )
        );

        when(transactionRepository.existsByReservationIdAndStatus("res-12345", TransactionStatus.SUCCESS))
                .thenReturn(false);

        assertThatThrownBy(() -> paymentService.processPayment(expiredCardRequest, userId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Card has expired");
    }

    @Test
    @DisplayName("Should retrieve payment history for user")
    void testGetUserPaymentHistory() {
        Transaction tx1 = Transaction.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .reservationId("res-1")
                .amount(new BigDecimal("10.00"))
                .currency("USD")
                .status(TransactionStatus.SUCCESS)
                .transactionRef("TXN-20260816-001")
                .processedAt(Instant.now())
                .build();

        when(transactionRepository.findByUserIdOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(tx1));

        List<PaymentResponse> history = paymentService.getUserPaymentHistory(userId);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).reservationId()).isEqualTo("res-1");
    }

    @Test
    @DisplayName("Should successfully refund a SUCCESS transaction")
    void testRefundTransaction_Success() {
        UUID txId = UUID.randomUUID();
        Transaction tx = Transaction.builder()
                .id(txId)
                .userId(userId)
                .reservationId("res-1")
                .amount(new BigDecimal("20.00"))
                .currency("USD")
                .status(TransactionStatus.SUCCESS)
                .transactionRef("TXN-20260816-001")
                .processedAt(Instant.now())
                .build();

        when(transactionRepository.findById(txId)).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        PaymentResponse response = paymentService.refundTransaction(txId);

        assertThat(response.status()).isEqualTo("REFUNDED");
        verify(eventPublisher, times(1)).publishPaymentStatus(tx);
    }

    @Test
    @DisplayName("Should fail refund if transaction is not in SUCCESS state")
    void testRefundTransaction_InvalidState() {
        UUID txId = UUID.randomUUID();
        Transaction tx = Transaction.builder()
                .id(txId)
                .userId(userId)
                .reservationId("res-1")
                .amount(new BigDecimal("20.00"))
                .currency("USD")
                .status(TransactionStatus.FAILED)
                .build();

        when(transactionRepository.findById(txId)).thenReturn(Optional.of(tx));

        assertThatThrownBy(() -> paymentService.refundTransaction(txId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only successful transactions can be refunded");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when receipt does not exist")
    void testGetReceipt_NotFound() {
        UUID txId = UUID.randomUUID();
        Transaction tx = Transaction.builder()
                .id(txId)
                .userId(userId)
                .reservationId("res-1")
                .amount(new BigDecimal("10.00"))
                .currency("USD")
                .status(TransactionStatus.SUCCESS)
                .build();

        when(transactionRepository.findById(txId)).thenReturn(Optional.of(tx));
        when(receiptRepository.findByTransactionId(txId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getReceiptByTransactionId(txId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Receipt not found for transaction");
    }
}
