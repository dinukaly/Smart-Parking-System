package com.spms.parkingservice.controller;

import com.spms.parkingservice.dto.request.CreateParkingSpaceRequest;
import com.spms.parkingservice.dto.request.IotEventRequest;
import com.spms.parkingservice.dto.request.UpdateParkingSpaceRequest;
import com.spms.parkingservice.dto.request.UpdateSpaceStatusRequest;
import com.spms.parkingservice.dto.response.IotEventResponse;
import com.spms.parkingservice.dto.response.PagedResponse;
import com.spms.parkingservice.dto.response.ParkingSpaceResponse;
import com.spms.parkingservice.model.entity.enums.SpaceStatus;
import com.spms.parkingservice.model.entity.enums.SpaceType;
import com.spms.parkingservice.security.SecurityUtils;
import com.spms.parkingservice.service.IotService;
import com.spms.parkingservice.service.ParkingSpaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/parking/spaces")
@RequiredArgsConstructor
@Tag(name = "Parking Spaces", description = "Endpoints for managing and searching parking spaces")
public class ParkingSpaceController {

    private final ParkingSpaceService parkingSpaceService;
    private final IotService iotService;

    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Create a new parking space", description = "Allows space owners or admins to register a new parking space with location coordinates")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<ParkingSpaceResponse> createParkingSpace(
            @Valid @RequestBody CreateParkingSpaceRequest request
    ) {
        UUID ownerId = SecurityUtils.getCurrentUserId();
        ParkingSpaceResponse response = parkingSpaceService.createParkingSpace(request, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Search parking spaces", description = "Public endpoint to search and filter parking spaces with optional PostGIS geospatial radius search")
    public ResponseEntity<PagedResponse<ParkingSpaceResponse>> searchSpaces(
            @RequestParam(required = false) @Parameter(description = "City name") String city,
            @RequestParam(required = false) @Parameter(description = "Zone name") String zone,
            @RequestParam(required = false) @Parameter(description = "Space status (AVAILABLE, OCCUPIED, RESERVED, MAINTENANCE)") SpaceStatus status,
            @RequestParam(required = false) @Parameter(description = "Space type (STANDARD, HANDICAP, EV, VIP)") SpaceType type,
            @RequestParam(required = false) @Parameter(description = "User latitude for proximity search") Double latitude,
            @RequestParam(required = false) @Parameter(description = "User longitude for proximity search") Double longitude,
            @RequestParam(required = false) @Parameter(description = "Search radius in metres (e.g. 5000 for 5km)") Double radius,
            @RequestParam(defaultValue = "1") @Parameter(description = "Page number (1-indexed)") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "Page size limit") int limit
    ) {
        PagedResponse<ParkingSpaceResponse> response = parkingSpaceService.searchSpaces(
                city, zone, status, type, latitude, longitude, radius, page, limit
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get parking space by ID", description = "Public endpoint to retrieve details of a specific parking space")
    public ResponseEntity<ParkingSpaceResponse> getParkingSpaceById(@PathVariable UUID id) {
        ParkingSpaceResponse response = parkingSpaceService.getParkingSpaceById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Update parking space", description = "Allows space owners or admins to update parking space details")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<ParkingSpaceResponse> updateParkingSpace(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateParkingSpaceRequest request
    ) {
        UUID requesterId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.isAdmin();
        ParkingSpaceResponse response = parkingSpaceService.updateParkingSpace(id, request, requesterId, isAdmin);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Update parking space status", description = "Manually update the status of a parking space (e.g. AVAILABLE, OCCUPIED, MAINTENANCE)")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<ParkingSpaceResponse> updateSpaceStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSpaceStatusRequest request
    ) {
        UUID requesterId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.isAdmin();
        ParkingSpaceResponse response = parkingSpaceService.updateSpaceStatus(id, request, requesterId, isAdmin);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Delete parking space", description = "Allows space owners or admins to delete a parking space")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<Void> deleteParkingSpace(@PathVariable UUID id) {
        UUID requesterId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.isAdmin();
        parkingSpaceService.deleteParkingSpace(id, requesterId, isAdmin);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/iot")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    @Operation(summary = "Simulate IoT sensor event", description = "Simulate an IoT sensor event (ENTRY, EXIT, STATUS_UPDATE) for a parking space")
    @SecurityRequirement(name = "BearerAuth")
    public ResponseEntity<IotEventResponse> processIotEvent(
            @PathVariable UUID id,
            @Valid @RequestBody IotEventRequest request
    ) {
        UUID requesterId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.isAdmin();
        IotEventResponse response = iotService.processIotEvent(id, request, requesterId, isAdmin);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
