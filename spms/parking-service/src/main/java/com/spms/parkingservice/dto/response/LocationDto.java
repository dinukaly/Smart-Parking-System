package com.spms.parkingservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationDto implements Serializable {
    private String city;
    private String address;
    private String zone;
    private Double latitude;
    private Double longitude;
}
