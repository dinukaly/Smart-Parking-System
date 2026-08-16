package com.spms.parkingservice.service;

import com.spms.parkingservice.dto.request.IotEventRequest;
import com.spms.parkingservice.dto.response.IotEventResponse;
import com.spms.parkingservice.model.entity.IotEvent;

import java.util.UUID;

/**
 * Service interface defining IoT sensor event processing.
 */
public interface IotService {

    IotEventResponse processIotEvent(UUID spaceId, IotEventRequest request, UUID requesterId, boolean isAdmin);

    IotEventResponse mapToResponse(IotEvent event);
}
