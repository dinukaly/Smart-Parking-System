package com.spms.parkingservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.spms.parkingservice.dto.request.CreateParkingSpaceRequest;
import com.spms.parkingservice.dto.request.IotEventRequest;
import com.spms.parkingservice.dto.request.UpdateParkingSpaceRequest;
import com.spms.parkingservice.dto.request.UpdateSpaceStatusRequest;
import com.spms.parkingservice.dto.response.IotEventResponse;
import com.spms.parkingservice.dto.response.LocationDto;
import com.spms.parkingservice.dto.response.PagedResponse;
import com.spms.parkingservice.dto.response.ParkingSpaceResponse;
import com.spms.parkingservice.exception.GlobalExceptionHandler;
import com.spms.parkingservice.model.entity.enums.IotEventType;
import com.spms.parkingservice.model.entity.enums.SpaceStatus;
import com.spms.parkingservice.model.entity.enums.SpaceType;
import com.spms.parkingservice.service.IotService;
import com.spms.parkingservice.service.ParkingSpaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ParkingSpaceControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ParkingSpaceService parkingSpaceService;

    @Mock
    private IotService iotService;

    @InjectMocks
    private ParkingSpaceController parkingSpaceController;

    private UUID spaceId;
    private ParkingSpaceResponse mockResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(parkingSpaceController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        spaceId = UUID.randomUUID();

        LocationDto loc = LocationDto.builder()
                .city("Colombo")
                .address("123 Main Street")
                .zone("Zone A")
                .latitude(6.9271)
                .longitude(79.8612)
                .build();

        mockResponse = ParkingSpaceResponse.builder()
                .id(spaceId)
                .ownerId(UUID.randomUUID())
                .name("Zone A - Spot 12")
                .description("Covered parking spot")
                .location(loc)
                .spaceType(SpaceType.STANDARD)
                .vehicleTypes(List.of("CAR"))
                .pricePerHour(BigDecimal.valueOf(2.50))
                .status(SpaceStatus.AVAILABLE)
                .features(List.of("covered", "cctv"))
                .iotEnabled(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void createParkingSpace_returns201() throws Exception {
        CreateParkingSpaceRequest request = CreateParkingSpaceRequest.builder()
                .name("Zone A - Spot 12")
                .city("Colombo")
                .address("123 Main Street")
                .latitude(6.9271)
                .longitude(79.8612)
                .pricePerHour(BigDecimal.valueOf(2.50))
                .build();

        when(parkingSpaceService.createParkingSpace(any(), any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/parking/spaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(spaceId.toString()))
                .andExpect(jsonPath("$.name").value("Zone A - Spot 12"));
    }

    @Test
    void searchSpaces_public_returns200() throws Exception {
        PagedResponse<ParkingSpaceResponse> pagedResponse = PagedResponse.<ParkingSpaceResponse>builder()
                .total(1)
                .page(1)
                .limit(20)
                .data(List.of(mockResponse))
                .build();

        when(parkingSpaceService.searchSpaces(any(), any(), any(), any(), any(), any(), any(), eq(1), eq(20)))
                .thenReturn(pagedResponse);

        mockMvc.perform(get("/api/parking/spaces?city=Colombo&page=1&limit=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Zone A - Spot 12"));
    }

    @Test
    void getParkingSpaceById_public_returns200() throws Exception {
        when(parkingSpaceService.getParkingSpaceById(spaceId)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/parking/spaces/" + spaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(spaceId.toString()))
                .andExpect(jsonPath("$.name").value("Zone A - Spot 12"));
    }

    @Test
    void updateParkingSpace_returns200() throws Exception {
        UpdateParkingSpaceRequest request = UpdateParkingSpaceRequest.builder()
                .name("Zone A - Spot 12 Updated")
                .build();

        when(parkingSpaceService.updateParkingSpace(eq(spaceId), any(), any(), anyBoolean()))
                .thenReturn(mockResponse);

        mockMvc.perform(put("/api/parking/spaces/" + spaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(spaceId.toString()));
    }

    @Test
    void updateSpaceStatus_returns200() throws Exception {
        UpdateSpaceStatusRequest request = UpdateSpaceStatusRequest.builder()
                .status(SpaceStatus.OCCUPIED)
                .build();

        when(parkingSpaceService.updateSpaceStatus(eq(spaceId), any(), any(), anyBoolean()))
                .thenReturn(mockResponse);

        mockMvc.perform(patch("/api/parking/spaces/" + spaceId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteParkingSpace_returns204() throws Exception {
        mockMvc.perform(delete("/api/parking/spaces/" + spaceId))
                .andExpect(status().isNoContent());
    }

    @Test
    void processIotEvent_returns201() throws Exception {
        IotEventRequest request = IotEventRequest.builder()
                .eventType(IotEventType.ENTRY)
                .licensePlate("ABC-1234")
                .sensorId("SENSOR-001")
                .build();

        IotEventResponse iotResponse = IotEventResponse.builder()
                .id(UUID.randomUUID())
                .parkingSpaceId(spaceId)
                .eventType(IotEventType.ENTRY)
                .licensePlate("ABC-1234")
                .sensorId("SENSOR-001")
                .createdAt(Instant.now())
                .build();

        when(iotService.processIotEvent(eq(spaceId), any(), any(), anyBoolean()))
                .thenReturn(iotResponse);

        mockMvc.perform(post("/api/parking/spaces/" + spaceId + "/iot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventType").value("ENTRY"))
                .andExpect(jsonPath("$.licensePlate").value("ABC-1234"));
    }
}
