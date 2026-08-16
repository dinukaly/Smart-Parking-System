package com.spms.parkingservice.service;

import com.spms.parkingservice.dto.request.CreateParkingSpaceRequest;
import com.spms.parkingservice.dto.request.UpdateParkingSpaceRequest;
import com.spms.parkingservice.dto.request.UpdateSpaceStatusRequest;
import com.spms.parkingservice.dto.response.PagedResponse;
import com.spms.parkingservice.dto.response.ParkingSpaceResponse;
import com.spms.parkingservice.exception.ForbiddenException;
import com.spms.parkingservice.exception.ResourceNotFoundException;
import com.spms.parkingservice.messaging.EventPublisher;
import com.spms.parkingservice.model.entity.ParkingSpace;
import com.spms.parkingservice.model.entity.enums.SpaceStatus;
import com.spms.parkingservice.model.entity.enums.SpaceType;
import com.spms.parkingservice.repository.ParkingSpaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

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

import com.spms.parkingservice.service.impl.ParkingSpaceServiceImpl;

@ExtendWith(MockitoExtension.class)
class ParkingSpaceServiceTest {

    @Mock
    private ParkingSpaceRepository parkingSpaceRepository;

    @Spy
    private GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private ParkingSpaceServiceImpl parkingSpaceService;

    private UUID spaceId;
    private UUID ownerId;
    private ParkingSpace mockSpace;

    @BeforeEach
    void setUp() {
        spaceId = UUID.randomUUID();
        ownerId = UUID.randomUUID();

        Point point = geometryFactory.createPoint(new Coordinate(79.8612, 6.9271));
        point.setSRID(4326);

        mockSpace = ParkingSpace.builder()
                .id(spaceId)
                .ownerId(ownerId)
                .name("Zone A - Spot 12")
                .description("Covered slot")
                .city("Colombo")
                .address("123 Main Street")
                .zone("Zone A")
                .location(point)
                .spaceType(SpaceType.STANDARD)
                .vehicleTypes(List.of("CAR"))
                .pricePerHour(BigDecimal.valueOf(2.50))
                .status(SpaceStatus.AVAILABLE)
                .features(List.of("covered", "cctv"))
                .iotEnabled(true)
                .build();
        mockSpace.setCreatedAt(Instant.now());
        mockSpace.setUpdatedAt(Instant.now());
    }

    @Test
    void createParkingSpace_success() {
        CreateParkingSpaceRequest request = CreateParkingSpaceRequest.builder()
                .name("Zone A - Spot 12")
                .description("Covered slot")
                .city("Colombo")
                .address("123 Main Street")
                .zone("Zone A")
                .latitude(6.9271)
                .longitude(79.8612)
                .spaceType(SpaceType.STANDARD)
                .vehicleTypes(List.of("CAR"))
                .pricePerHour(BigDecimal.valueOf(2.50))
                .features(List.of("covered", "cctv"))
                .iotEnabled(true)
                .build();

        when(parkingSpaceRepository.save(any(ParkingSpace.class))).thenReturn(mockSpace);

        ParkingSpaceResponse response = parkingSpaceService.createParkingSpace(request, ownerId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(spaceId);
        assertThat(response.getOwnerId()).isEqualTo(ownerId);
        assertThat(response.getName()).isEqualTo("Zone A - Spot 12");
        assertThat(response.getLocation().getCity()).isEqualTo("Colombo");
        assertThat(response.getLocation().getLatitude()).isEqualTo(6.9271);
        assertThat(response.getLocation().getLongitude()).isEqualTo(79.8612);
        assertThat(response.getStatus()).isEqualTo(SpaceStatus.AVAILABLE);

        verify(parkingSpaceRepository).save(any(ParkingSpace.class));
    }

    @Test
    void searchSpaces_success() {
        Page<ParkingSpace> page = new PageImpl<>(List.of(mockSpace));
        when(parkingSpaceRepository.searchSpaces(
                eq("Colombo"), eq("Zone A"), eq("AVAILABLE"), eq("STANDARD"),
                eq(79.8612), eq(6.9271), eq(5000.0), any(Pageable.class)
        )).thenReturn(page);
        when(parkingSpaceRepository.calculateDistanceMetres(eq(spaceId), eq(79.8612), eq(6.9271)))
                .thenReturn(150.0);

        PagedResponse<ParkingSpaceResponse> response = parkingSpaceService.searchSpaces(
                "Colombo", "Zone A", SpaceStatus.AVAILABLE, SpaceType.STANDARD,
                6.9271, 79.8612, 5000.0, 1, 20
        );

        assertThat(response).isNotNull();
        assertThat(response.getTotal()).isEqualTo(1);
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getName()).isEqualTo("Zone A - Spot 12");
        assertThat(response.getData().get(0).getDistanceMetres()).isEqualTo(150.0);
    }

    @Test
    void getParkingSpaceById_found() {
        when(parkingSpaceRepository.findById(spaceId)).thenReturn(Optional.of(mockSpace));

        ParkingSpaceResponse response = parkingSpaceService.getParkingSpaceById(spaceId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(spaceId);
        assertThat(response.getName()).isEqualTo("Zone A - Spot 12");
    }

    @Test
    void getParkingSpaceById_notFound() {
        when(parkingSpaceRepository.findById(spaceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> parkingSpaceService.getParkingSpaceById(spaceId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Parking space not found");
    }

    @Test
    void updateParkingSpace_byOwner_success() {
        when(parkingSpaceRepository.findById(spaceId)).thenReturn(Optional.of(mockSpace));
        when(parkingSpaceRepository.save(any(ParkingSpace.class))).thenReturn(mockSpace);

        UpdateParkingSpaceRequest updateReq = UpdateParkingSpaceRequest.builder()
                .name("Zone A - Updated Spot")
                .pricePerHour(BigDecimal.valueOf(3.00))
                .build();

        ParkingSpaceResponse response = parkingSpaceService.updateParkingSpace(spaceId, updateReq, ownerId, false);

        assertThat(response).isNotNull();
        verify(parkingSpaceRepository).save(mockSpace);
    }

    @Test
    void updateParkingSpace_byUnauthorizedUser_throwsForbidden() {
        when(parkingSpaceRepository.findById(spaceId)).thenReturn(Optional.of(mockSpace));

        UpdateParkingSpaceRequest updateReq = UpdateParkingSpaceRequest.builder()
                .name("Zone A - Updated Spot")
                .build();

        UUID anotherUser = UUID.randomUUID();
        assertThatThrownBy(() -> parkingSpaceService.updateParkingSpace(spaceId, updateReq, anotherUser, false))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("permission");
    }

    @Test
    void updateSpaceStatus_success() {
        when(parkingSpaceRepository.findById(spaceId)).thenReturn(Optional.of(mockSpace));
        when(parkingSpaceRepository.save(any(ParkingSpace.class))).thenReturn(mockSpace);

        UpdateSpaceStatusRequest req = UpdateSpaceStatusRequest.builder()
                .status(SpaceStatus.OCCUPIED)
                .build();

        ParkingSpaceResponse response = parkingSpaceService.updateSpaceStatus(spaceId, req, ownerId, false);

        assertThat(response).isNotNull();
        verify(eventPublisher).publishParkingStatusUpdated(any());
    }

    @Test
    void deleteParkingSpace_success() {
        when(parkingSpaceRepository.findById(spaceId)).thenReturn(Optional.of(mockSpace));

        parkingSpaceService.deleteParkingSpace(spaceId, ownerId, false);

        verify(parkingSpaceRepository).delete(mockSpace);
    }
}
