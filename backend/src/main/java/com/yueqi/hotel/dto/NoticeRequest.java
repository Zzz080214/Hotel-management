package com.yueqi.hotel.dto;

import jakarta.validation.constraints.NotBlank;

public record NoticeRequest(
        @NotBlank String title,
        @NotBlank String level,
        @NotBlank String content,
        Boolean published) {
}
