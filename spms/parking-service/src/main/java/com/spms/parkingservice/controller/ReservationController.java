package com.spms.parkingservice.controller;

import com.spms.parkingservice.dto.request.CreateReservationRequest;
import com.spms.parkingservice.dto.response.PagedResponse;
import com.spms.parkingservice.dto.response.ReservationResponse;
import com.spms.parkingservice.security.SecurityUtils;
import com.spms.parkingservice.service.ReservationService;
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
@RequestMapping("/api/parking/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservations", description = "Endpoints for managing parking space reservations")
@SecurityRequirement(name = "BearerAuth")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a parking reservation", description = "Book an available parking space for a specific time window")
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody CreateReservationRequest request
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        ReservationResponse response = reservationService.createReservation(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List user reservations", description = "Retrieve all reservations belonging to the authenticated user")
    public ResponseEntity<PagedResponse<ReservationResponse>> getUserReservations(
            @RequestParam(defaultValue = "1") @Parameter(description = "Page number (1-indexed)") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "Page size limit") int limit
    ) {
        UUID userId = SecurityUtils.getCurrentUserId();
        PagedResponse<ReservationResponse> response = reservationService.getUserReservations(userId, page, limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get reservation details", description = "Retrieve details of a specific reservation")
    public ResponseEntity<ReservationResponse> getReservationById(@PathVariable UUID id) {
        UUID requesterId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.isAdmin();
        ReservationResponse response = reservationService.getReservationById(id, requesterId, isAdmin);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancel a reservation", description = "Cancel an active or pending parking reservation")
    public ResponseEntity<ReservationResponse> cancelReservation(@PathVariable UUID id) {
        UUID requesterId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.isAdmin();
        ReservationResponse response = reservationService.cancelReservation(id, requesterId, isAdmin);
        return ResponseEntity.ok(response);
    }
}
