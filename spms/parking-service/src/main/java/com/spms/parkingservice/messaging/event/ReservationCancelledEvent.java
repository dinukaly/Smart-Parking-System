package com.spms.parkingservice.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationCancelledEvent implements Serializable {
    private String eventType; // "RESERVATION_CANCELLED"
    private UUID reservationId;
    private UUID userId;
    private UUID parkingSpaceId;
    private Instant timestamp;
}
