package com.spms.parkingservice.model.entity;

import com.spms.parkingservice.model.entity.enums.IotEventType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity for iot_events table.
 * Records IoT sensor events (ENTRY/EXIT/STATUS_UPDATE) for a parking space.
 **/
@Entity
@Table(
    name = "iot_events",
    indexes = {
        @Index(name = "idx_iot_events_space_id", columnList = "parking_space_id")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IotEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * FK to parking_spaces table
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parking_space_id", nullable = false)
    private ParkingSpace parkingSpace;

    /**
     * Logical FK to vehicle-service. No DB-level constraint — cross-service reference.
     */
    @Column(name = "vehicle_id")
    private UUID vehicleId;

    @Column(name = "license_plate", length = 20)
    private String licensePlate;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private IotEventType eventType;

    @Column(name = "sensor_id", length = 100)
    private String sensorId;

    /**
     * Sensor detection confidence score: 0.000 to 1.000.
     */
    @Column(name = "confidence", precision = 4, scale = 3)
    private BigDecimal confidence;

    /**
     * Immutable creation timestamp set by JPA auditing.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;
}
