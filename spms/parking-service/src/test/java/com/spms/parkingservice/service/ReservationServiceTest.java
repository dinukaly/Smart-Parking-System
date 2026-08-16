package com.spms.parkingservice.service;

import com.spms.parkingservice.dto.request.CreateReservationRequest;
import com.spms.parkingservice.dto.response.PagedResponse;
import com.spms.parkingservice.dto.response.ReservationResponse;
import com.spms.parkingservice.exception.BadRequestException;
import com.spms.parkingservice.exception.ConflictException;
import com.spms.parkingservice.exception.ForbiddenException;
import com.spms.parkingservice.exception.ResourceNotFoundException;
import com.spms.parkingservice.messaging.EventPublisher;
import com.spms.parkingservice.model.entity.ParkingSpace;
import com.spms.parkingservice.model.entity.Reservation;
import com.spms.parkingservice.model.entity.enums.PaymentStatus;
import com.spms.parkingservice.model.entity.enums.ReservationStatus;
import com.spms.parkingservice.model.entity.enums.SpaceStatus;
import com.spms.parkingservice.model.entity.enums.SpaceType;
import com.spms.parkingservice.repository.ParkingSpaceRepository;
import com.spms.parkingservice.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.spms.parkingservice.service.impl.ReservationServiceImpl;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ParkingSpaceRepository parkingSpaceRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    private UUID userId;
    private UUID spaceId;
    private UUID vehicleId;
    private UUID reservationId;
    private ParkingSpace mockSpace;
    private Reservation mockReservation;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        spaceId = UUID.randomUUID();
        vehicleId = UUID.randomUUID();
        reservationId = UUID.randomUUID();

        mockSpace = ParkingSpace.builder()
                .id(spaceId)
                .ownerId(UUID.randomUUID())
                .name("Zone A - Spot 12")
                .city("Colombo")
                .address("123 Main Street")
                .spaceType(SpaceType.STANDARD)
                .pricePerHour(BigDecimal.valueOf(2.50))
                .status(SpaceStatus.AVAILABLE)
                .build();
        mockSpace.setCreatedAt(Instant.now());

        mockReservation = Reservation.builder()
                .id(reservationId)
                .userId(userId)
                .vehicleId(vehicleId)
                .parkingSpace(mockSpace)
                .status(ReservationStatus.ACTIVE)
                .startTime(OffsetDateTime.now().plusHours(1))
                .endTime(OffsetDateTime.now().plusHours(3))
                .totalAmount(BigDecimal.valueOf(5.00))
                .paymentStatus(PaymentStatus.PENDING)
                .build();
        mockReservation.setCreatedAt(Instant.now());
    }

    @Test
    void createReservation_success() {
        CreateReservationRequest request = CreateReservationRequest.builder()
                .parkingSpaceId(spaceId)
                .vehicleId(vehicleId)
                .startTime(OffsetDateTime.now().plusHours(1))
                .endTime(OffsetDateTime.now().plusHours(3))
                .build();

        when(parkingSpaceRepository.findById(spaceId)).thenReturn(Optional.of(mockSpace));
        when(reservationRepository.findOverlappingReservations(eq(spaceId), any(), any())).thenReturn(List.of());
        when(reservationRepository.save(any(Reservation.class))).thenReturn(mockReservation);

        ReservationResponse response = reservationService.createReservation(request, userId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(reservationId);
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getParkingSpaceId()).isEqualTo(spaceId);
        assertThat(response.getStatus()).isEqualTo(ReservationStatus.ACTIVE);

        verify(eventPublisher).publishParkingReserved(any());
    }

    @Test
    void createReservation_overlapping_throwsConflict() {
        CreateReservationRequest request = CreateReservationRequest.builder()
                .parkingSpaceId(spaceId)
                .vehicleId(vehicleId)
                .startTime(OffsetDateTime.now().plusHours(1))
                .endTime(OffsetDateTime.now().plusHours(3))
                .build();

        when(parkingSpaceRepository.findById(spaceId)).thenReturn(Optional.of(mockSpace));
        when(reservationRepository.findOverlappingReservations(eq(spaceId), any(), any()))
                .thenReturn(List.of(mockReservation));

        assertThatThrownBy(() -> reservationService.createReservation(request, userId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already reserved");
    }

    @Test
    void createReservation_invalidTimes_throwsBadRequest() {
        CreateReservationRequest request = CreateReservationRequest.builder()
                .parkingSpaceId(spaceId)
                .vehicleId(vehicleId)
                .startTime(OffsetDateTime.now().plusHours(3))
                .endTime(OffsetDateTime.now().plusHours(1))
                .build();

        assertThatThrownBy(() -> reservationService.createReservation(request, userId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Start time must be before end time");
    }

    @Test
    void getUserReservations_success() {
        Page<Reservation> page = new PageImpl<>(List.of(mockReservation));
        when(reservationRepository.findByUserId(eq(userId), any(Pageable.class))).thenReturn(page);

        PagedResponse<ReservationResponse> response = reservationService.getUserReservations(userId, 1, 20);

        assertThat(response).isNotNull();
        assertThat(response.getTotal()).isEqualTo(1);
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getId()).isEqualTo(reservationId);
    }

    @Test
    void getReservationById_found() {
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(mockReservation));

        ReservationResponse response = reservationService.getReservationById(reservationId, userId, false);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(reservationId);
    }

    @Test
    void getReservationById_unauthorized_throwsForbidden() {
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(mockReservation));

        UUID otherUser = UUID.randomUUID();
        assertThatThrownBy(() -> reservationService.getReservationById(reservationId, otherUser, false))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("permission");
    }

    @Test
    void cancelReservation_success() {
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(mockReservation));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(mockReservation);

        ReservationResponse response = reservationService.cancelReservation(reservationId, userId, false);

        assertThat(response).isNotNull();
        assertThat(mockReservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        verify(eventPublisher).publishReservationCancelled(any());
    }
}
