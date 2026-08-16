package com.spms.parkingservice.dto.request;

import com.spms.parkingservice.model.entity.enums.SpaceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSpaceStatusRequest {

    @NotNull(message = "Status is required")
    private SpaceStatus status;
}
