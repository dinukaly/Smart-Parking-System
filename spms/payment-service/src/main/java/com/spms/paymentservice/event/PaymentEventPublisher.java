package com.spms.paymentservice.event;

import com.spms.paymentservice.model.entity.Transaction;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Publishes payment-related domain events to the shared SPMS RabbitMQ Topic Exchange.
 *
 * Routing key pattern: payment.{status_lowercase}
 *   e.g.  payment.completed
 *         payment.failed
 *         payment.refunded
 */
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${spms.rabbitmq.exchange:spms.events}")
    private String exchangeName;

    /**
     * Publish a payment status event after a transaction state change.
     *
     * @param transaction the persisted transaction whose status changed
     */
    public void publishPaymentStatus(Transaction transaction) {
        String routingKey = "payment." + transaction.getStatus().name().toLowerCase();

        PaymentStatusEvent event = new PaymentStatusEvent(
                transaction.getId(),
                transaction.getReservationId(),
                transaction.getUserId(),
                transaction.getStatus().name(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getTransactionRef(),
                Instant.now()
        );

        try {
            rabbitTemplate.convertAndSend(exchangeName, routingKey, event);
            log.info("Published PaymentStatusEvent [txId={}, status={}, routingKey={}]",
                    transaction.getId(), transaction.getStatus(), routingKey);
        } catch (Exception e) {
            // Log but do not fail the payment — event publishing is best-effort
            log.error("Failed to publish PaymentStatusEvent for txId={}: {}",
                    transaction.getId(), e.getMessage(), e);
        }
    }
}
