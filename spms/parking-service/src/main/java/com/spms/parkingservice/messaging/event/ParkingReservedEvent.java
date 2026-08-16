package com.spms.parkingservice.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingReservedEvent implements Serializable {
    private String eventType; // "PARKING_RESERVED"
    private UUID reservationId;
    private UUID userId;
    private UUID vehicleId;
    private UUID parkingSpaceId;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private BigDecimal totalAmount;
    private Instant timestamp;
}
