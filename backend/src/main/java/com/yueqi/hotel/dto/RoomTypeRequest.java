package com.yueqi.hotel.dto;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RoomTypeRequest(
        @NotBlank String name,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        @NotBlank String area,
        @NotBlank String bed,
        String breakfast,
        String occupancy,
        @NotBlank String status,
        String tag,
        String image,
        String summary,
        @NotNull @Min(0) Integer totalRooms,
        @NotNull @Min(0) Integer availableRooms,
        List<String> features) {
}
