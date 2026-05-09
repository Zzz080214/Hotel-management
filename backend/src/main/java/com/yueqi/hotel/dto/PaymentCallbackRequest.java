package com.yueqi.hotel.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentCallbackRequest(
        @NotBlank String orderId,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String transactionId,
        @NotBlank String status) {
}
