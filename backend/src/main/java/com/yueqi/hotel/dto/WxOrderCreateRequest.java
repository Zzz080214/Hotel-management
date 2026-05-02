package com.yueqi.hotel.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record WxOrderCreateRequest(
        @NotNull Long roomTypeId,
        String roomTypeName,
        @NotBlank String guestName,
        @NotBlank @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String guestPhone,
        @NotNull @Min(1) Integer stayNights,
        @NotNull @DecimalMin("0.01") BigDecimal totalAmount,
        @NotNull LocalDate checkInDate,
        @NotNull LocalDate checkOutDate) {
}
