package com.spms.parkingservice.service;

import com.spms.parkingservice.dto.request.CreateParkingSpaceRequest;
import com.spms.parkingservice.dto.request.UpdateParkingSpaceRequest;
import com.spms.parkingservice.dto.request.UpdateSpaceStatusRequest;
import com.spms.parkingservice.dto.response.PagedResponse;
import com.spms.parkingservice.dto.response.ParkingSpaceResponse;
import com.spms.parkingservice.model.entity.ParkingSpace;
import com.spms.parkingservice.model.entity.enums.SpaceStatus;
import com.spms.parkingservice.model.entity.enums.SpaceType;

import java.util.UUID;

/**
 * Service interface defining parking space business operations.
 */
public interface ParkingSpaceService {

    ParkingSpaceResponse createParkingSpace(CreateParkingSpaceRequest request, UUID ownerId);

    PagedResponse<ParkingSpaceResponse> searchSpaces(
            String city,
            String zone,
            SpaceStatus status,
            SpaceType type,
            Double latitude,
            Double longitude,
            Double radiusMetres,
            int page,
            int limit
    );

    ParkingSpaceResponse getParkingSpaceById(UUID id);

    ParkingSpaceResponse updateParkingSpace(UUID id, UpdateParkingSpaceRequest request, UUID requesterId, boolean isAdmin);

    ParkingSpaceResponse updateSpaceStatus(UUID id, UpdateSpaceStatusRequest request, UUID requesterId, boolean isAdmin);

    void deleteParkingSpace(UUID id, UUID requesterId, boolean isAdmin);

    ParkingSpace findEntityById(UUID id);

    ParkingSpaceResponse mapToResponse(ParkingSpace space, Double originLng, Double originLat);
}
