package com.spms.parkingservice.dto.response;

import com.spms.parkingservice.model.entity.enums.IotEventType;
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
public class IotEventResponse implements Serializable {

    private UUID id;
    private UUID parkingSpaceId;
    private UUID vehicleId;
    private String licensePlate;
    private IotEventType eventType;
    private String sensorId;
    private BigDecimal confidence;
    private Instant createdAt;
}
