package com.yueqi.hotel.dto;

import jakarta.validation.constraints.NotBlank;

public record UserUpsertRequest(
        @NotBlank String username,
        String password,
        @NotBlank String role,
        String displayName,
        Boolean enabled) {
}
