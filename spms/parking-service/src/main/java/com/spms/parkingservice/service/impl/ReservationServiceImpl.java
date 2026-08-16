package com.spms.parkingservice.service.impl;

import com.spms.parkingservice.dto.request.CreateReservationRequest;
import com.spms.parkingservice.dto.response.PagedResponse;
import com.spms.parkingservice.dto.response.ReservationResponse;
import com.spms.parkingservice.exception.BadRequestException;
import com.spms.parkingservice.exception.ConflictException;
import com.spms.parkingservice.exception.ForbiddenException;
import com.spms.parkingservice.exception.ResourceNotFoundException;
import com.spms.parkingservice.messaging.EventPublisher;
import com.spms.parkingservice.messaging.event.ParkingReservedEvent;
import com.spms.parkingservice.messaging.event.ReservationCancelledEvent;
import com.spms.parkingservice.model.entity.ParkingSpace;
import com.spms.parkingservice.model.entity.Reservation;
import com.spms.parkingservice.model.entity.enums.PaymentStatus;
import com.spms.parkingservice.model.entity.enums.ReservationStatus;
import com.spms.parkingservice.model.entity.enums.SpaceStatus;
import com.spms.parkingservice.repository.ParkingSpaceRepository;
import com.spms.parkingservice.repository.ReservationRepository;
import com.spms.parkingservice.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    private final ParkingSpaceRepository parkingSpaceRepository;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    @CacheEvict(value = {"parkingSpaces", "parkingAvailability"}, allEntries = true)
    public ReservationResponse createReservation(CreateReservationRequest request, UUID userId) {
        log.info("User {} attempting to create reservation for space {}", userId, request.getParkingSpaceId());

        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new BadRequestException("Start time and end time are required");
        }

        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new BadRequestException("Start time must be before end time");
        }

        ParkingSpace space = parkingSpaceRepository.findById(request.getParkingSpaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Parking space not found with id: " + request.getParkingSpaceId()));

        if (space.getStatus() == SpaceStatus.MAINTENANCE) {
            throw new BadRequestException("Parking space is currently under maintenance");
        }

        // Check for conflicting overlapping reservations
        List<Reservation> overlapping = reservationRepository.findOverlappingReservations(
                space.getId(),
                request.getStartTime(),
                request.getEndTime()
        );

        if (!overlapping.isEmpty()) {
            throw new ConflictException("Parking space is already reserved for the selected time slot");
        }

        // Calculate total amount based on duration in hours * price per hour
        long minutes = Duration.between(request.getStartTime(), request.getEndTime()).toMinutes();
        double hours = Math.max(0.5, (double) minutes / 60.0);
        BigDecimal totalAmount = space.getPricePerHour()
                .multiply(BigDecimal.valueOf(hours))
                .setScale(2, RoundingMode.HALF_UP);

        Reservation reservation = Reservation.builder()
                .userId(userId)
                .vehicleId(request.getVehicleId())
                .parkingSpace(space)
                .status(ReservationStatus.ACTIVE)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .totalAmount(totalAmount)
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        Reservation saved = reservationRepository.save(reservation);

        // Update space status to RESERVED if reservation starts now or soon
        if (space.getStatus() == SpaceStatus.AVAILABLE) {
            space.setStatus(SpaceStatus.RESERVED);
            parkingSpaceRepository.save(space);
        }

        // Publish event to RabbitMQ
        eventPublisher.publishParkingReserved(ParkingReservedEvent.builder()
                .eventType("PARKING_RESERVED")
                .reservationId(saved.getId())
                .userId(userId)
                .vehicleId(request.getVehicleId())
                .parkingSpaceId(space.getId())
                .startTime(saved.getStartTime())
                .endTime(saved.getEndTime())
                .totalAmount(totalAmount)
                .timestamp(Instant.now())
                .build());

        log.info("Reservation {} created successfully for user {}", saved.getId(), userId);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ReservationResponse> getUserReservations(UUID userId, int page, int limit) {
        int pageIndex = Math.max(0, page - 1);
        int pageSize = limit > 0 ? limit : 20;
        Pageable pageable = PageRequest.of(pageIndex, pageSize);

        Page<Reservation> reservationPage = reservationRepository.findByUserId(userId, pageable);

        List<ReservationResponse> dtoList = reservationPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return PagedResponse.<ReservationResponse>builder()
                .total(reservationPage.getTotalElements())
                .page(pageIndex + 1)
                .limit(pageSize)
                .data(dtoList)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(UUID id, UUID requesterId, boolean isAdmin) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));

        if (!isAdmin && (requesterId == null || !reservation.getUserId().equals(requesterId))) {
            throw new ForbiddenException("You do not have permission to view this reservation");
        }

        return mapToResponse(reservation);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"parkingSpaces", "parkingAvailability"}, allEntries = true)
    public ReservationResponse cancelReservation(UUID id, UUID requesterId, boolean isAdmin) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));

        if (!isAdmin && (requesterId == null || !reservation.getUserId().equals(requesterId))) {
            throw new ForbiddenException("You do not have permission to cancel this reservation");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BadRequestException("Reservation is already cancelled");
        }

        if (reservation.getStatus() == ReservationStatus.COMPLETED) {
            throw new BadRequestException("Cannot cancel a completed reservation");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        Reservation updated = reservationRepository.save(reservation);

        // If space is currently marked RESERVED, free it back to AVAILABLE
        ParkingSpace space = reservation.getParkingSpace();
        if (space != null && space.getStatus() == SpaceStatus.RESERVED) {
            space.setStatus(SpaceStatus.AVAILABLE);
            parkingSpaceRepository.save(space);
        }

        // Publish cancellation event to RabbitMQ
        eventPublisher.publishReservationCancelled(ReservationCancelledEvent.builder()
                .eventType("RESERVATION_CANCELLED")
                .reservationId(updated.getId())
                .userId(reservation.getUserId())
                .parkingSpaceId(space != null ? space.getId() : null)
                .timestamp(Instant.now())
                .build());

        log.info("Reservation {} cancelled successfully", id);
        return mapToResponse(updated);
    }

    @Override
    public ReservationResponse mapToResponse(Reservation reservation) {
        return ReservationResponse.builder()
                .id(reservation.getId())
                .userId(reservation.getUserId())
                .vehicleId(reservation.getVehicleId())
                .parkingSpaceId(reservation.getParkingSpace() != null ? reservation.getParkingSpace().getId() : null)
                .status(reservation.getStatus())
                .startTime(reservation.getStartTime())
                .endTime(reservation.getEndTime())
                .actualEntryTime(reservation.getActualEntryTime())
                .actualExitTime(reservation.getActualExitTime())
                .totalAmount(reservation.getTotalAmount())
                .paymentStatus(reservation.getPaymentStatus())
                .transactionId(reservation.getTransactionId())
                .createdAt(reservation.getCreatedAt())
                .updatedAt(reservation.getUpdatedAt())
                .build();
    }
}
