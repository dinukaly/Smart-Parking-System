package com.spms.parkingservice.dto.request;

import com.spms.parkingservice.model.entity.enums.SpaceStatus;
import com.spms.parkingservice.model.entity.enums.SpaceType;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateParkingSpaceRequest {

    private String name;

    private String description;

    private String city;

    private String address;

    private String zone;

    private Double latitude;

    private Double longitude;

    private SpaceType spaceType;

    private List<String> vehicleTypes;

    @DecimalMin(value = "0.0", inclusive = false, message = "Price per hour must be greater than 0")
    private BigDecimal pricePerHour;

    private SpaceStatus status;

    private List<String> features;

    private Boolean iotEnabled;
}
