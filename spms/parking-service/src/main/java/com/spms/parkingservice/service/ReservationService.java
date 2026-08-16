package com.spms.parkingservice.service;

import com.spms.parkingservice.dto.request.CreateReservationRequest;
import com.spms.parkingservice.dto.response.PagedResponse;
import com.spms.parkingservice.dto.response.ReservationResponse;
import com.spms.parkingservice.model.entity.Reservation;

import java.util.UUID;

/**
 * Service interface defining reservation lifecycle operations.
 */
public interface ReservationService {

    ReservationResponse createReservation(CreateReservationRequest request, UUID userId);

    PagedResponse<ReservationResponse> getUserReservations(UUID userId, int page, int limit);

    ReservationResponse getReservationById(UUID id, UUID requesterId, boolean isAdmin);

    ReservationResponse cancelReservation(UUID id, UUID requesterId, boolean isAdmin);

    ReservationResponse mapToResponse(Reservation reservation);
}
