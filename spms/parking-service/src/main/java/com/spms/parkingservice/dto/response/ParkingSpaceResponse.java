package com.spms.parkingservice.dto.response;

import com.spms.parkingservice.model.entity.enums.SpaceStatus;
import com.spms.parkingservice.model.entity.enums.SpaceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingSpaceResponse implements Serializable {

    private UUID id;
    private UUID ownerId;
    private String name;
    private String description;
    private LocationDto location;
    private SpaceType spaceType;
    private List<String> vehicleTypes;
    private BigDecimal pricePerHour;
    private SpaceStatus status;
    private List<String> features;
    private Boolean iotEnabled;
    private Double distanceMetres;
    private Instant createdAt;
    private Instant updatedAt;
}
