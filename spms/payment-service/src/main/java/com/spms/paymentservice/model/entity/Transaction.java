package com.spms.paymentservice.model.entity;

import com.spms.paymentservice.model.entity.enums.PaymentMethod;
import com.spms.paymentservice.model.entity.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a monetary payment transaction for parking reservations.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "receipt")
@EqualsAndHashCode(callSuper = false, exclude = "receipt")
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transactions_user_id", columnList = "user_id"),
        @Index(name = "idx_transactions_reservation_id", columnList = "reservation_id"),
        @Index(name = "idx_transactions_status", columnList = "status"),
        @Index(name = "idx_transactions_transaction_ref", columnList = "transaction_ref", unique = true)
})
public class Transaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "reservation_id", nullable = false, length = 255)
    private String reservationId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 3, nullable = false)
    @Builder.Default
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 50)
    @Builder.Default
    private PaymentMethod paymentMethod = PaymentMethod.MOCK_CARD;

    @Column(name = "card_last_four", length = 4)
    private String cardLastFour;

    @Column(name = "transaction_ref", unique = true, length = 255)
    private String transactionRef;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @OneToOne(mappedBy = "transaction", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Receipt receipt;
}
