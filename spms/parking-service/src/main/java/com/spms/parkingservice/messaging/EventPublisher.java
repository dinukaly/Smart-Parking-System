package com.spms.parkingservice.messaging;

import com.spms.parkingservice.messaging.event.ParkingReservedEvent;
import com.spms.parkingservice.messaging.event.ParkingStatusUpdatedEvent;
import com.spms.parkingservice.messaging.event.ReservationCancelledEvent;
import com.spms.parkingservice.messaging.event.VehicleMovementEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Event publisher for publishing domain events to RabbitMQ topic exchange (spms.events)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${spms.rabbitmq.exchange:spms.events}")
    private String exchangeName;

    public static final String ROUTING_KEY_PARKING_RESERVED = "parking.reserved";
    public static final String ROUTING_KEY_RESERVATION_CANCELLED = "reservation.cancelled";
    public static final String ROUTING_KEY_VEHICLE_ENTERED = "vehicle.entered";
    public static final String ROUTING_KEY_VEHICLE_EXITED = "vehicle.exited";
    public static final String ROUTING_KEY_PARKING_STATUS = "parking.status.updated";

    public void publishParkingReserved(ParkingReservedEvent event) {
        log.info("Publishing event {} with routing key {} to exchange {}", event, ROUTING_KEY_PARKING_RESERVED, exchangeName);
        try {
            rabbitTemplate.convertAndSend(exchangeName, ROUTING_KEY_PARKING_RESERVED, event);
        } catch (Exception ex) {
            log.error("Failed to publish parking.reserved event: {}", ex.getMessage(), ex);
        }
    }

    public void publishReservationCancelled(ReservationCancelledEvent event) {
        log.info("Publishing event {} with routing key {} to exchange {}", event, ROUTING_KEY_RESERVATION_CANCELLED, exchangeName);
        try {
            rabbitTemplate.convertAndSend(exchangeName, ROUTING_KEY_RESERVATION_CANCELLED, event);
        } catch (Exception ex) {
            log.error("Failed to publish reservation.cancelled event: {}", ex.getMessage(), ex);
        }
    }

    public void publishVehicleMovement(VehicleMovementEvent event, boolean isEntry) {
        String routingKey = isEntry ? ROUTING_KEY_VEHICLE_ENTERED : ROUTING_KEY_VEHICLE_EXITED;
        log.info("Publishing event {} with routing key {} to exchange {}", event, routingKey, exchangeName);
        try {
            rabbitTemplate.convertAndSend(exchangeName, routingKey, event);
        } catch (Exception ex) {
            log.error("Failed to publish {} event: {}", routingKey, ex.getMessage(), ex);
        }
    }

    public void publishParkingStatusUpdated(ParkingStatusUpdatedEvent event) {
        log.info("Publishing event {} with routing key {} to exchange {}", event, ROUTING_KEY_PARKING_STATUS, exchangeName);
        try {
            rabbitTemplate.convertAndSend(exchangeName, ROUTING_KEY_PARKING_STATUS, event);
        } catch (Exception ex) {
            log.error("Failed to publish parking.status.updated event: {}", ex.getMessage(), ex);
        }
    }
}
