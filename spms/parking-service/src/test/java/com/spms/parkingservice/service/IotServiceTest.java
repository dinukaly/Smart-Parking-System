package com.spms.parkingservice.service;

import com.spms.parkingservice.dto.request.IotEventRequest;
import com.spms.parkingservice.dto.response.IotEventResponse;
import com.spms.parkingservice.exception.ForbiddenException;
import com.spms.parkingservice.messaging.EventPublisher;
import com.spms.parkingservice.model.entity.IotEvent;
import com.spms.parkingservice.model.entity.ParkingSpace;
import com.spms.parkingservice.model.entity.Reservation;
import com.spms.parkingservice.model.entity.enums.IotEventType;
import com.spms.parkingservice.model.entity.enums.ReservationStatus;
import com.spms.parkingservice.model.entity.enums.SpaceStatus;
import com.spms.parkingservice.repository.IotEventRepository;
import com.spms.parkingservice.repository.ParkingSpaceRepository;
import com.spms.parkingservice.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.spms.parkingservice.service.impl.IotServiceImpl;

@ExtendWith(MockitoExtension.class)
class IotServiceTest {

    @Mock
    private IotEventRepository iotEventRepository;

    @Mock
    private ParkingSpaceRepository parkingSpaceRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private IotServiceImpl iotService;

    private UUID spaceId;
    private UUID ownerId;
    private ParkingSpace mockSpace;
    private Reservation mockReservation;

    @BeforeEach
    void setUp() {
        spaceId = UUID.randomUUID();
        ownerId = UUID.randomUUID();

        mockSpace = ParkingSpace.builder()
                .id(spaceId)
                .ownerId(ownerId)
                .name("Zone A - Spot 12")
                .status(SpaceStatus.AVAILABLE)
                .build();

        mockReservation = Reservation.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .vehicleId(UUID.randomUUID())
                .parkingSpace(mockSpace)
                .status(ReservationStatus.ACTIVE)
                .build();
    }

    @Test
    void processIotEvent_entry_success() {
        IotEventRequest request = IotEventRequest.builder()
                .eventType(IotEventType.ENTRY)
                .licensePlate("ABC-1234")
                .sensorId("SENSOR-001")
                .confidence(BigDecimal.valueOf(0.985))
                .build();

        IotEvent mockSavedEvent = IotEvent.builder()
                .id(UUID.randomUUID())
                .parkingSpace(mockSpace)
                .eventType(IotEventType.ENTRY)
                .licensePlate("ABC-1234")
                .sensorId("SENSOR-001")
                .confidence(BigDecimal.valueOf(0.985))
                .createdAt(Instant.now())
                .build();

        when(parkingSpaceRepository.findById(spaceId)).thenReturn(Optional.of(mockSpace));
        when(reservationRepository.findActiveReservationsForSpace(eq(spaceId), any()))
                .thenReturn(List.of(mockReservation));
        when(iotEventRepository.save(any(IotEvent.class))).thenReturn(mockSavedEvent);

        IotEventResponse response = iotService.processIotEvent(spaceId, request, ownerId, false);

        assertThat(response).isNotNull();
        assertThat(response.getEventType()).isEqualTo(IotEventType.ENTRY);
        assertThat(response.getLicensePlate()).isEqualTo("ABC-1234");
        assertThat(mockSpace.getStatus()).isEqualTo(SpaceStatus.OCCUPIED);

        verify(eventPublisher).publishVehicleMovement(any(), eq(true));
        verify(parkingSpaceRepository).save(mockSpace);
    }

    @Test
    void processIotEvent_exit_success() {
        mockSpace.setStatus(SpaceStatus.OCCUPIED);

        IotEventRequest request = IotEventRequest.builder()
                .eventType(IotEventType.EXIT)
                .licensePlate("ABC-1234")
                .sensorId("SENSOR-001")
                .confidence(BigDecimal.valueOf(0.990))
                .build();

        IotEvent mockSavedEvent = IotEvent.builder()
                .id(UUID.randomUUID())
                .parkingSpace(mockSpace)
                .eventType(IotEventType.EXIT)
                .licensePlate("ABC-1234")
                .sensorId("SENSOR-001")
                .confidence(BigDecimal.valueOf(0.990))
                .createdAt(Instant.now())
                .build();

        when(parkingSpaceRepository.findById(spaceId)).thenReturn(Optional.of(mockSpace));
        when(reservationRepository.findActiveReservationsForSpace(eq(spaceId), any()))
                .thenReturn(List.of(mockReservation));
        when(iotEventRepository.save(any(IotEvent.class))).thenReturn(mockSavedEvent);

        IotEventResponse response = iotService.processIotEvent(spaceId, request, ownerId, false);

        assertThat(response).isNotNull();
        assertThat(response.getEventType()).isEqualTo(IotEventType.EXIT);
        assertThat(mockSpace.getStatus()).isEqualTo(SpaceStatus.AVAILABLE);
        assertThat(mockReservation.getStatus()).isEqualTo(ReservationStatus.COMPLETED);

        verify(eventPublisher).publishVehicleMovement(any(), eq(false));
    }

    @Test
    void processIotEvent_unauthorized_throwsForbidden() {
        IotEventRequest request = IotEventRequest.builder()
                .eventType(IotEventType.ENTRY)
                .build();

        when(parkingSpaceRepository.findById(spaceId)).thenReturn(Optional.of(mockSpace));

        UUID randomUser = UUID.randomUUID();
        assertThatThrownBy(() -> iotService.processIotEvent(spaceId, request, randomUser, false))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("permission");
    }
}
