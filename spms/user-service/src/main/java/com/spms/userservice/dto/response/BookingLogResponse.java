package com.spms.userservice.dto.response;

import com.spms.userservice.model.entity.enums.BookingAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingLogResponse {

    private UUID id;
    private String reservationId;
    private BookingAction action;
    private Instant createdAt;
}
