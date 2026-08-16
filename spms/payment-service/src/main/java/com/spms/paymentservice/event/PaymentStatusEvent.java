package com.spms.paymentservice.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published to RabbitMQ when a payment status changes.
 * Routing key: payment.{status}  (e.g. payment.completed, payment.failed, payment.refunded)
 */
public record PaymentStatusEvent(
        UUID transactionId,
        String reservationId,
        UUID userId,
        String status,
        BigDecimal amount,
        String currency,
        String transactionRef,
        Instant occurredAt
) {}
