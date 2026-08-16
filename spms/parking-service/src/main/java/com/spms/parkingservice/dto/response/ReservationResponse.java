package com.spms.parkingservice.dto.response;

import com.spms.parkingservice.model.entity.enums.PaymentStatus;
import com.spms.parkingservice.model.entity.enums.ReservationStatus;
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
public class ReservationResponse implements Serializable {

    private UUID id;
    private UUID userId;
    private UUID vehicleId;
    private UUID parkingSpaceId;
    private ReservationStatus status;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private OffsetDateTime actualEntryTime;
    private OffsetDateTime actualExitTime;
    private BigDecimal totalAmount;
    private PaymentStatus paymentStatus;
    private UUID transactionId;
    private Instant createdAt;
    private Instant updatedAt;
}
