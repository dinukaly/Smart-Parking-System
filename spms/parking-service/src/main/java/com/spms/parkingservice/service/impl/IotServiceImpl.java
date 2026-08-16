package com.spms.parkingservice.service.impl;

import com.spms.parkingservice.dto.request.IotEventRequest;
import com.spms.parkingservice.dto.response.IotEventResponse;
import com.spms.parkingservice.exception.ForbiddenException;
import com.spms.parkingservice.exception.ResourceNotFoundException;
import com.spms.parkingservice.messaging.EventPublisher;
import com.spms.parkingservice.messaging.event.VehicleMovementEvent;
import com.spms.parkingservice.model.entity.IotEvent;
import com.spms.parkingservice.model.entity.ParkingSpace;
import com.spms.parkingservice.model.entity.Reservation;
import com.spms.parkingservice.model.entity.enums.IotEventType;
import com.spms.parkingservice.model.entity.enums.ReservationStatus;
import com.spms.parkingservice.model.entity.enums.SpaceStatus;
import com.spms.parkingservice.repository.IotEventRepository;
import com.spms.parkingservice.repository.ParkingSpaceRepository;
import com.spms.parkingservice.repository.ReservationRepository;
import com.spms.parkingservice.service.IotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IotServiceImpl implements IotService {

    private final IotEventRepository iotEventRepository;
    private final ParkingSpaceRepository parkingSpaceRepository;
    private final ReservationRepository reservationRepository;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    @CacheEvict(value = {"parkingSpaces", "parkingAvailability"}, allEntries = true)
    public IotEventResponse processIotEvent(UUID spaceId, IotEventRequest request, UUID requesterId, boolean isAdmin) {
        log.info("Processing IoT event {} for space {}", request.getEventType(), spaceId);

        ParkingSpace space = parkingSpaceRepository.findById(spaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Parking space not found with id: " + spaceId));

        if (!isAdmin && (requesterId == null || !space.getOwnerId().equals(requesterId))) {
            throw new ForbiddenException("You do not have permission to simulate IoT events for this parking space");
        }

        // Find active reservation if exists
        List<Reservation> activeReservations = reservationRepository.findActiveReservationsForSpace(spaceId, null);
        Reservation currentReservation = activeReservations.isEmpty() ? null : activeReservations.get(0);
        UUID vehicleId = currentReservation != null ? currentReservation.getVehicleId() : null;

        IotEvent event = IotEvent.builder()
                .parkingSpace(space)
                .vehicleId(vehicleId)
                .licensePlate(request.getLicensePlate())
                .eventType(request.getEventType())
                .sensorId(request.getSensorId())
                .confidence(request.getConfidence())
                .build();

        IotEvent savedEvent = iotEventRepository.save(event);

        OffsetDateTime nowOffset = OffsetDateTime.now();
        Instant nowInstant = Instant.now();

        if (request.getEventType() == IotEventType.ENTRY) {
            space.setStatus(SpaceStatus.OCCUPIED);
            parkingSpaceRepository.save(space);

            if (currentReservation != null) {
                currentReservation.setActualEntryTime(nowOffset);
                reservationRepository.save(currentReservation);
            }

            eventPublisher.publishVehicleMovement(VehicleMovementEvent.builder()
                    .eventType("VEHICLE_ENTERED")
                    .parkingSpaceId(spaceId)
                    .vehicleId(vehicleId)
                    .licensePlate(request.getLicensePlate())
                    .sensorId(request.getSensorId())
                    .confidence(request.getConfidence())
                    .timestamp(nowInstant)
                    .build(), true);

        } else if (request.getEventType() == IotEventType.EXIT) {
            space.setStatus(SpaceStatus.AVAILABLE);
            parkingSpaceRepository.save(space);

            if (currentReservation != null) {
                currentReservation.setActualExitTime(nowOffset);
                currentReservation.setStatus(ReservationStatus.COMPLETED);
                reservationRepository.save(currentReservation);
            }

            eventPublisher.publishVehicleMovement(VehicleMovementEvent.builder()
                    .eventType("VEHICLE_EXITED")
                    .parkingSpaceId(spaceId)
                    .vehicleId(vehicleId)
                    .licensePlate(request.getLicensePlate())
                    .sensorId(request.getSensorId())
                    .confidence(request.getConfidence())
                    .timestamp(nowInstant)
                    .build(), false);
        }

        log.info("IoT event processed successfully with id {}", savedEvent.getId());
        return mapToResponse(savedEvent);
    }

    @Override
    public IotEventResponse mapToResponse(IotEvent event) {
        return IotEventResponse.builder()
                .id(event.getId())
                .parkingSpaceId(event.getParkingSpace() != null ? event.getParkingSpace().getId() : null)
                .vehicleId(event.getVehicleId())
                .licensePlate(event.getLicensePlate())
                .eventType(event.getEventType())
                .sensorId(event.getSensorId())
                .confidence(event.getConfidence())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
