package com.yueqi.hotel.dto;

public record WxLoginResponse(
        String token,
        String openid,
        String nickname,
        String avatarUrl) {
}
