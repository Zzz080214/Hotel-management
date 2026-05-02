package com.yueqi.hotel.controller.auth;

import java.util.Map;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yueqi.hotel.common.ApiResponse;
import com.yueqi.hotel.dto.LoginRequest;
import com.yueqi.hotel.dto.LoginResponse;
import com.yueqi.hotel.entity.User;
import com.yueqi.hotel.service.AuthService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String HEADER_AUTH = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String token = extractToken(request);
        if (token != null) {
            authService.logout(token);
        }
        return ApiResponse.success();
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, String>> currentUser(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            return ApiResponse.fail(401, "未登录");
        }
        Optional<User> userOpt = authService.validateToken(token);
        return userOpt
                .map(user -> ApiResponse.success(Map.of(
                        "username", user.getUsername(),
                        "role", user.getRole(),
                        "displayName", user.getDisplayName())))
                .orElseGet(() -> ApiResponse.fail(401, "登录已过期"));
    }

    static String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER_AUTH);
        if (header != null && header.startsWith(TOKEN_PREFIX)) {
            return header.substring(TOKEN_PREFIX.length());
        }
        return null;
    }
}
