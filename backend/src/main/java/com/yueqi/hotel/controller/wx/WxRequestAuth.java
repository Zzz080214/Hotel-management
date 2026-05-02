package com.yueqi.hotel.controller.wx;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.yueqi.hotel.service.WxAuthService;
import com.yueqi.hotel.service.WxAuthService.WxSession;

final class WxRequestAuth {

    private static final String HEADER_AUTH = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    private WxRequestAuth() {
    }

    static Optional<String> extractToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER_AUTH);
        if (StringUtils.hasText(header) && header.startsWith(TOKEN_PREFIX)) {
            return Optional.of(header.substring(TOKEN_PREFIX.length()).trim());
        }
        return Optional.empty();
    }

    static WxSession requireSession(HttpServletRequest request, WxAuthService authService) {
        String token = extractToken(request)
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "请先微信登录"));
        return authService.validateToken(token)
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "微信登录已过期，请重新登录"));
    }
}
