package com.spms.parkingservice.repository;

import com.spms.parkingservice.model.entity.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    Page<Reservation> findByUserId(UUID userId, Pageable pageable);

    List<Reservation> findByUserId(UUID userId);

    Page<Reservation> findByParkingSpaceId(UUID parkingSpaceId, Pageable pageable);

    /**
     * Checks if there are overlapping active or pending reservations for the space.
     */
    @Query("""
        SELECT r FROM Reservation r
        WHERE r.parkingSpace.id = :parkingSpaceId
          AND r.status IN (com.spms.parkingservice.model.entity.enums.ReservationStatus.PENDING, com.spms.parkingservice.model.entity.enums.ReservationStatus.ACTIVE)
          AND r.startTime < :endTime
          AND r.endTime > :startTime
        """)
    List<Reservation> findOverlappingReservations(
        @Param("parkingSpaceId") UUID parkingSpaceId,
        @Param("startTime") OffsetDateTime startTime,
        @Param("endTime") OffsetDateTime endTime
    );

    /**
     * Finds active or pending reservations for a parking space (optional vehicle filter).
     */
    @Query("""
        SELECT r FROM Reservation r
        WHERE r.parkingSpace.id = :parkingSpaceId
          AND r.status IN (com.spms.parkingservice.model.entity.enums.ReservationStatus.PENDING, com.spms.parkingservice.model.entity.enums.ReservationStatus.ACTIVE)
          AND (:vehicleId IS NULL OR r.vehicleId = :vehicleId)
        ORDER BY r.startTime ASC
        """)
    List<Reservation> findActiveReservationsForSpace(
        @Param("parkingSpaceId") UUID parkingSpaceId,
        @Param("vehicleId") UUID vehicleId
    );
}
