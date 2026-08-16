package com.spms.parkingservice.dto.request;

import com.spms.parkingservice.model.entity.enums.IotEventType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IotEventRequest {

    @NotNull(message = "Event type is required")
    private IotEventType eventType;

    private String licensePlate;

    private String sensorId;

    private BigDecimal confidence;
}
