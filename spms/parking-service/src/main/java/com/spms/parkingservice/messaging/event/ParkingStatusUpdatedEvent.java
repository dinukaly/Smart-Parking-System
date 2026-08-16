package com.spms.parkingservice.messaging.event;

import com.spms.parkingservice.model.entity.enums.SpaceStatus;
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
public class ParkingStatusUpdatedEvent implements Serializable {
    private String eventType; // "PARKING_STATUS_UPDATED"
    private UUID parkingSpaceId;
    private SpaceStatus previousStatus;
    private SpaceStatus newStatus;
    private Instant timestamp;
}
