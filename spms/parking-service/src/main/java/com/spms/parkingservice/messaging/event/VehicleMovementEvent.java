package com.spms.parkingservice.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleMovementEvent implements Serializable {
    private String eventType; // "VEHICLE_ENTERED" or "VEHICLE_EXITED"
    private UUID parkingSpaceId;
    private UUID vehicleId;
    private String licensePlate;
    private String sensorId;
    private BigDecimal confidence;
    private Instant timestamp;
}
