package com.spms.paymentservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request payload to initiate a payment for a reservation.
 */
public record ProcessPaymentRequest(

        @NotBlank(message = "Reservation ID must not be blank")
        String reservationId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        String currency,

        @NotBlank(message = "Payment method is required")
        String paymentMethod,

        @Valid
        CardDetails cardDetails

) {
    /**
     * Mock card details — validated only when paymentMethod == MOCK_CARD.
     */
    public record CardDetails(

            @NotBlank(message = "Card number is required")
            String cardNumber,

            @NotBlank(message = "Expiry month is required")
            String expiryMonth,

            @NotBlank(message = "Expiry year is required")
            String expiryYear,

            @NotBlank(message = "CVV is required")
            String cvv,

            @NotBlank(message = "Cardholder name is required")
            String cardholderName
    ) {}
}
