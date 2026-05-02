package com.yueqi.hotel.config;

import java.io.IOException;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yueqi.hotel.common.ApiResponse;
import com.yueqi.hotel.entity.User;
import com.yueqi.hotel.service.AuthService;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final String HEADER_AUTH = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String ATTR_USER = "currentUser";

    private final AuthService authService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        String token = extractToken(request);
        if (token == null) {
            writeUnauthorized(response, "未提供认证令牌");
            return false;
        }
        Optional<User> userOpt = authService.validateToken(token);
        if (userOpt.isEmpty()) {
            writeUnauthorized(response, "登录已过期，请重新登录");
            return false;
        }
        request.setAttribute(ATTR_USER, userOpt.get());
        return true;
    }

    /** 从请求中获取当前用户（供 Controller 调用） */
    public static User currentUser(HttpServletRequest request) {
        return (User) request.getAttribute(ATTR_USER);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER_AUTH);
        if (header != null && header.startsWith(TOKEN_PREFIX)) {
            return header.substring(TOKEN_PREFIX.length());
        }
        return null;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.fail(401, message)));
    }
}
