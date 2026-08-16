package com.spms.parkingservice.model.entity;

import com.spms.parkingservice.model.entity.enums.SpaceStatus;
import com.spms.parkingservice.model.entity.enums.SpaceType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity for parking_spaces table.
 * Uses PostGIS GEOMETRY(Point, 4326) for geographic location (longitude, latitude).
 */
@Entity
@Table(
    name = "parking_spaces",
    indexes = {
        @Index(name = "idx_parking_spaces_city_status", columnList = "city, status"),
        @Index(name = "idx_parking_spaces_owner_id",   columnList = "owner_id"),
        @Index(name = "idx_parking_spaces_status",     columnList = "status")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ParkingSpace extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Logical FK to user-service (OWNER role user). No DB-level constraint — cross-service reference.
     */
    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "zone", length = 100)
    private String zone;

    /**
     * PostGIS geometry point (SRID 4326 — WGS84 longitude/latitude).
     * Coordinate order: (longitude, latitude).
     * Use GeometryFactory.createPoint(new Coordinate(lng, lat)) to construct.
     */
    @Column(name = "location", columnDefinition = "geometry(Point,4326)")
    private Point location;

    @Enumerated(EnumType.STRING)
    @Column(name = "space_type", nullable = false, length = 20)
    @Builder.Default
    private SpaceType spaceType = SpaceType.STANDARD;

    /**
     * Supported vehicle types: CAR, MOTORCYCLE, TRUCK.
     * Stored as a PostgreSQL TEXT[] array.
     */
    @ElementCollection
    @CollectionTable(name = "parking_space_vehicle_types",
                     joinColumns = @JoinColumn(name = "parking_space_id"))
    @Column(name = "vehicle_type")
    @Builder.Default
    private List<String> vehicleTypes = new ArrayList<>(List.of("CAR"));

    @Column(name = "price_per_hour", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerHour;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SpaceStatus status = SpaceStatus.AVAILABLE;

    /**
     * Space amenity features: e.g. ['covered','cctv','24h'].
     * Stored as a PostgreSQL TEXT[] array.
     */
    @ElementCollection
    @CollectionTable(name = "parking_space_features",
                     joinColumns = @JoinColumn(name = "parking_space_id"))
    @Column(name = "feature")
    @Builder.Default
    private List<String> features = new ArrayList<>();

    @Column(name = "iot_enabled")
    @Builder.Default
    private Boolean iotEnabled = false;

    /**
     * Reservations associated with this parking space.
     * Bidirectional for convenience; mappedBy prevents double table creation.
     */
    @OneToMany(mappedBy = "parkingSpace", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Reservation> reservations = new ArrayList<>();

    /**
     * IoT events associated with this parking space.
     */
    @OneToMany(mappedBy = "parkingSpace", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<IotEvent> iotEvents = new ArrayList<>();
}
