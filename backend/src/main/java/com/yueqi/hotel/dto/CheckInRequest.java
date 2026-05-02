package com.yueqi.hotel.dto;

import jakarta.validation.constraints.NotBlank;

public record CheckInRequest(@NotBlank String roomNo) {
}
