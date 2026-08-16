package com.spms.parkingservice.repository;

import com.spms.parkingservice.model.entity.ParkingSpace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ParkingSpaceRepository extends JpaRepository<ParkingSpace, UUID> {

    Page<ParkingSpace> findByOwnerId(UUID ownerId, Pageable pageable);

    List<ParkingSpace> findByCityIgnoreCase(String city);

    List<ParkingSpace> findByZoneIgnoreCase(String zone);

    /**
     * Search parking spaces with optional filters and PostGIS geospatial radius search (ST_DWithin).
     * Calculates distance using ST_Distance when coordinates are provided.
     */
    @Query(value = """
        SELECT p.*
        FROM parking_spaces p
        WHERE (:city IS NULL OR LOWER(p.city) = LOWER(:city))
          AND (:zone IS NULL OR LOWER(p.zone) = LOWER(:zone))
          AND (:status IS NULL OR p.status = :status)
          AND (:spaceType IS NULL OR p.space_type = :spaceType)
          AND (
            :radiusMetres IS NULL OR :lng IS NULL OR :lat IS NULL OR
            ST_DWithin(p.location::geography, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusMetres)
          )
        ORDER BY
          CASE WHEN :lng IS NOT NULL AND :lat IS NOT NULL
               THEN ST_Distance(p.location::geography, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography)
          END ASC NULLS LAST,
          p.created_at DESC
        """,
        countQuery = """
        SELECT count(*)
        FROM parking_spaces p
        WHERE (:city IS NULL OR LOWER(p.city) = LOWER(:city))
          AND (:zone IS NULL OR LOWER(p.zone) = LOWER(:zone))
          AND (:status IS NULL OR p.status = :status)
          AND (:spaceType IS NULL OR p.space_type = :spaceType)
          AND (
            :radiusMetres IS NULL OR :lng IS NULL OR :lat IS NULL OR
            ST_DWithin(p.location::geography, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusMetres)
          )
        """,
        nativeQuery = true)
    Page<ParkingSpace> searchSpaces(
        @Param("city") String city,
        @Param("zone") String zone,
        @Param("status") String status,
        @Param("spaceType") String spaceType,
        @Param("lng") Double lng,
        @Param("lat") Double lat,
        @Param("radiusMetres") Double radiusMetres,
        Pageable pageable
    );

    /**
     * Calculates distance in metres between a parking space and target coordinates using PostGIS ST_Distance.
     */
    @Query(value = """
        SELECT ST_Distance(p.location::geography, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography)
        FROM parking_spaces p
        WHERE p.id = :spaceId
        """, nativeQuery = true)
    Double calculateDistanceMetres(
        @Param("spaceId") UUID spaceId,
        @Param("lng") Double lng,
        @Param("lat") Double lat
    );
}
