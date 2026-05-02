package com.yueqi.hotel.dto;

import jakarta.validation.constraints.NotBlank;

public record WxLoginRequest(
        @NotBlank String code,
        String nickname,
        String avatarUrl,
        String devOpenid) {
}
