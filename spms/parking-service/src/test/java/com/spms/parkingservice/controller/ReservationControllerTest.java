package com.spms.parkingservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.spms.parkingservice.dto.request.CreateReservationRequest;
import com.spms.parkingservice.dto.response.PagedResponse;
import com.spms.parkingservice.dto.response.ReservationResponse;
import com.spms.parkingservice.exception.GlobalExceptionHandler;
import com.spms.parkingservice.model.entity.enums.PaymentStatus;
import com.spms.parkingservice.model.entity.enums.ReservationStatus;
import com.spms.parkingservice.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ReservationControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ReservationService reservationService;

    @InjectMocks
    private ReservationController reservationController;

    private UUID reservationId;
    private UUID spaceId;
    private UUID vehicleId;
    private UUID userId;
    private ReservationResponse mockResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reservationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        reservationId = UUID.randomUUID();
        spaceId = UUID.randomUUID();
        vehicleId = UUID.randomUUID();
        userId = UUID.randomUUID();

        mockResponse = ReservationResponse.builder()
                .id(reservationId)
                .userId(userId)
                .vehicleId(vehicleId)
                .parkingSpaceId(spaceId)
                .status(ReservationStatus.ACTIVE)
                .startTime(OffsetDateTime.now().plusHours(1))
                .endTime(OffsetDateTime.now().plusHours(3))
                .totalAmount(BigDecimal.valueOf(5.00))
                .paymentStatus(PaymentStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void createReservation_returns201() throws Exception {
        CreateReservationRequest request = CreateReservationRequest.builder()
                .parkingSpaceId(spaceId)
                .vehicleId(vehicleId)
                .startTime(OffsetDateTime.now().plusHours(1))
                .endTime(OffsetDateTime.now().plusHours(3))
                .build();

        when(reservationService.createReservation(any(), any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/parking/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(reservationId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getUserReservations_returns200() throws Exception {
        PagedResponse<ReservationResponse> pagedResponse = PagedResponse.<ReservationResponse>builder()
                .total(1)
                .page(1)
                .limit(20)
                .data(List.of(mockResponse))
                .build();

        when(reservationService.getUserReservations(any(), eq(1), eq(20))).thenReturn(pagedResponse);

        mockMvc.perform(get("/api/parking/reservations?page=1&limit=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.data[0].id").value(reservationId.toString()));
    }

    @Test
    void getReservationById_returns200() throws Exception {
        when(reservationService.getReservationById(eq(reservationId), any(), anyBoolean())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/parking/reservations/" + reservationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservationId.toString()));
    }

    @Test
    void cancelReservation_returns200() throws Exception {
        mockResponse.setStatus(ReservationStatus.CANCELLED);
        when(reservationService.cancelReservation(eq(reservationId), any(), anyBoolean())).thenReturn(mockResponse);

        mockMvc.perform(patch("/api/parking/reservations/" + reservationId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
