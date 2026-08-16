package com.spms.paymentservice.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a digital receipt issued upon successful payment.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "transaction")
@EqualsAndHashCode(callSuper = false, exclude = "transaction")
@Entity
@Table(name = "receipts", indexes = {
        @Index(name = "idx_receipts_receipt_number", columnList = "receipt_number", unique = true),
        @Index(name = "idx_receipts_transaction_id", columnList = "transaction_id", unique = true)
})
public class Receipt extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false, unique = true)
    private Transaction transaction;

    @Column(name = "receipt_number", unique = true, nullable = false, length = 50)
    private String receiptNumber;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;
}
