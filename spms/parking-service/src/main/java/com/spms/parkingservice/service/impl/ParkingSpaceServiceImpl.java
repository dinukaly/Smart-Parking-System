package com.spms.parkingservice.service.impl;

import com.spms.parkingservice.dto.request.CreateParkingSpaceRequest;
import com.spms.parkingservice.dto.request.UpdateParkingSpaceRequest;
import com.spms.parkingservice.dto.request.UpdateSpaceStatusRequest;
import com.spms.parkingservice.dto.response.LocationDto;
import com.spms.parkingservice.dto.response.PagedResponse;
import com.spms.parkingservice.dto.response.ParkingSpaceResponse;
import com.spms.parkingservice.exception.ForbiddenException;
import com.spms.parkingservice.exception.ResourceNotFoundException;
import com.spms.parkingservice.messaging.EventPublisher;
import com.spms.parkingservice.messaging.event.ParkingStatusUpdatedEvent;
import com.spms.parkingservice.model.entity.ParkingSpace;
import com.spms.parkingservice.model.entity.enums.SpaceStatus;
import com.spms.parkingservice.model.entity.enums.SpaceType;
import com.spms.parkingservice.repository.ParkingSpaceRepository;
import com.spms.parkingservice.service.ParkingSpaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParkingSpaceServiceImpl implements ParkingSpaceService {

    private final ParkingSpaceRepository parkingSpaceRepository;
    private final GeometryFactory geometryFactory;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    @CacheEvict(value = {"parkingSpaces", "parkingAvailability"}, allEntries = true)
    public ParkingSpaceResponse createParkingSpace(CreateParkingSpaceRequest request, UUID ownerId) {
        log.info("Creating parking space '{}' for owner {}", request.getName(), ownerId);

        Point location = geometryFactory.createPoint(new Coordinate(request.getLongitude(), request.getLatitude()));
        location.setSRID(4326);

        ParkingSpace space = ParkingSpace.builder()
                .ownerId(ownerId)
                .name(request.getName())
                .description(request.getDescription())
                .city(request.getCity())
                .address(request.getAddress())
                .zone(request.getZone())
                .location(location)
                .spaceType(request.getSpaceType() != null ? request.getSpaceType() : SpaceType.STANDARD)
                .vehicleTypes(request.getVehicleTypes() != null && !request.getVehicleTypes().isEmpty()
                        ? new ArrayList<>(request.getVehicleTypes())
                        : new ArrayList<>(List.of("CAR")))
                .pricePerHour(request.getPricePerHour())
                .status(SpaceStatus.AVAILABLE)
                .features(request.getFeatures() != null ? new ArrayList<>(request.getFeatures()) : new ArrayList<>())
                .iotEnabled(Boolean.TRUE.equals(request.getIotEnabled()))
                .build();

        ParkingSpace saved = parkingSpaceRepository.save(space);
        log.info("Successfully created parking space with id: {}", saved.getId());
        return mapToResponse(saved, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ParkingSpaceResponse> searchSpaces(
            String city,
            String zone,
            SpaceStatus status,
            SpaceType type,
            Double latitude,
            Double longitude,
            Double radiusMetres,
            int page,
            int limit
    ) {
        int pageIndex = Math.max(0, page - 1);
        int pageSize = limit > 0 ? limit : 20;
        Pageable pageable = PageRequest.of(pageIndex, pageSize);

        String statusStr = status != null ? status.name() : null;
        String typeStr = type != null ? type.name() : null;

        Page<ParkingSpace> resultPage = parkingSpaceRepository.searchSpaces(
                city,
                zone,
                statusStr,
                typeStr,
                longitude,
                latitude,
                radiusMetres,
                pageable
        );

        List<ParkingSpaceResponse> dtoList = resultPage.getContent().stream()
                .map(space -> mapToResponse(space, longitude, latitude))
                .toList();

        return PagedResponse.<ParkingSpaceResponse>builder()
                .total(resultPage.getTotalElements())
                .page(pageIndex + 1)
                .limit(pageSize)
                .data(dtoList)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "parkingSpaces", key = "#id")
    public ParkingSpaceResponse getParkingSpaceById(UUID id) {
        ParkingSpace space = parkingSpaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parking space not found with id: " + id));
        return mapToResponse(space, null, null);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"parkingSpaces", "parkingAvailability"}, allEntries = true)
    public ParkingSpaceResponse updateParkingSpace(UUID id, UpdateParkingSpaceRequest request, UUID requesterId, boolean isAdmin) {
        ParkingSpace space = parkingSpaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parking space not found with id: " + id));

        validateOwnership(space, requesterId, isAdmin);

        if (request.getName() != null) space.setName(request.getName());
        if (request.getDescription() != null) space.setDescription(request.getDescription());
        if (request.getCity() != null) space.setCity(request.getCity());
        if (request.getAddress() != null) space.setAddress(request.getAddress());
        if (request.getZone() != null) space.setZone(request.getZone());
        if (request.getLatitude() != null && request.getLongitude() != null) {
            Point newLoc = geometryFactory.createPoint(new Coordinate(request.getLongitude(), request.getLatitude()));
            newLoc.setSRID(4326);
            space.setLocation(newLoc);
        }
        if (request.getSpaceType() != null) space.setSpaceType(request.getSpaceType());
        if (request.getVehicleTypes() != null) space.setVehicleTypes(new ArrayList<>(request.getVehicleTypes()));
        if (request.getPricePerHour() != null) space.setPricePerHour(request.getPricePerHour());
        if (request.getStatus() != null) space.setStatus(request.getStatus());
        if (request.getFeatures() != null) space.setFeatures(new ArrayList<>(request.getFeatures()));
        if (request.getIotEnabled() != null) space.setIotEnabled(request.getIotEnabled());

        ParkingSpace updated = parkingSpaceRepository.save(space);
        log.info("Updated parking space {}", id);
        return mapToResponse(updated, null, null);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"parkingSpaces", "parkingAvailability"}, allEntries = true)
    public ParkingSpaceResponse updateSpaceStatus(UUID id, UpdateSpaceStatusRequest request, UUID requesterId, boolean isAdmin) {
        ParkingSpace space = parkingSpaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parking space not found with id: " + id));

        validateOwnership(space, requesterId, isAdmin);

        SpaceStatus previousStatus = space.getStatus();
        space.setStatus(request.getStatus());
        ParkingSpace updated = parkingSpaceRepository.save(space);

        eventPublisher.publishParkingStatusUpdated(ParkingStatusUpdatedEvent.builder()
                .eventType("PARKING_STATUS_UPDATED")
                .parkingSpaceId(id)
                .previousStatus(previousStatus)
                .newStatus(request.getStatus())
                .timestamp(Instant.now())
                .build());

        log.info("Updated space {} status from {} to {}", id, previousStatus, request.getStatus());
        return mapToResponse(updated, null, null);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"parkingSpaces", "parkingAvailability"}, allEntries = true)
    public void deleteParkingSpace(UUID id, UUID requesterId, boolean isAdmin) {
        ParkingSpace space = parkingSpaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parking space not found with id: " + id));

        validateOwnership(space, requesterId, isAdmin);

        parkingSpaceRepository.delete(space);
        log.info("Deleted parking space {}", id);
    }

    @Override
    public ParkingSpace findEntityById(UUID id) {
        return parkingSpaceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Parking space not found with id: " + id));
    }

    private void validateOwnership(ParkingSpace space, UUID requesterId, boolean isAdmin) {
        if (!isAdmin && (requesterId == null || !space.getOwnerId().equals(requesterId))) {
            throw new ForbiddenException("You do not have permission to modify this parking space");
        }
    }

    @Override
    public ParkingSpaceResponse mapToResponse(ParkingSpace space, Double originLng, Double originLat) {
        Double lat = null;
        Double lng = null;
        if (space.getLocation() != null) {
            lat = space.getLocation().getY();
            lng = space.getLocation().getX();
        }

        Double distance = null;
        if (originLng != null && originLat != null && space.getId() != null) {
            distance = parkingSpaceRepository.calculateDistanceMetres(space.getId(), originLng, originLat);
        }

        LocationDto locationDto = LocationDto.builder()
                .city(space.getCity())
                .address(space.getAddress())
                .zone(space.getZone())
                .latitude(lat)
                .longitude(lng)
                .build();

        return ParkingSpaceResponse.builder()
                .id(space.getId())
                .ownerId(space.getOwnerId())
                .name(space.getName())
                .description(space.getDescription())
                .location(locationDto)
                .spaceType(space.getSpaceType())
                .vehicleTypes(space.getVehicleTypes())
                .pricePerHour(space.getPricePerHour())
                .status(space.getStatus())
                .features(space.getFeatures())
                .iotEnabled(space.getIotEnabled())
                .distanceMetres(distance)
                .createdAt(space.getCreatedAt())
                .updatedAt(space.getUpdatedAt())
                .build();
    }
}
