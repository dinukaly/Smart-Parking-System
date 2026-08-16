package com.spms.parkingservice.model.entity;

import com.spms.parkingservice.model.entity.enums.PaymentStatus;
import com.spms.parkingservice.model.entity.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity for reservations table.
 * Links a user + vehicle to a ParkingSpace for a specific time window.
 */
@Entity
@Table(
    name = "reservations",
    indexes = {
        @Index(name = "idx_reservations_user_id",         columnList = "user_id"),
        @Index(name = "idx_reservations_space_id_status", columnList = "parking_space_id, status"),
        @Index(name = "idx_reservations_start_end",       columnList = "start_time, end_time")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Reservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Logical FK to user-service.
     */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /**
     * Logical FK to vehicle-service.
     */
    @Column(name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    /**
     * FK to parking_spaces table (same database).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parking_space_id", nullable = false)
    private ParkingSpace parkingSpace;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.PENDING;

    @Column(name = "start_time", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime startTime;

    @Column(name = "end_time", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime endTime;

    @Column(name = "actual_entry_time", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime actualEntryTime;

    @Column(name = "actual_exit_time", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime actualExitTime;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    /**
     * Logical FK to payment-service transaction
     */
    @Column(name = "transaction_id")
    private UUID transactionId;
}
