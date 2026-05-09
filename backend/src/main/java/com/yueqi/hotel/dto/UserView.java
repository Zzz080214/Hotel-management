package com.yueqi.hotel.dto;

public record UserView(
        Long id,
        String username,
        String role,
        String displayName,
        Boolean enabled) {
}
