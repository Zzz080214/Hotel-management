package com.yueqi.hotel.service;

import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.yueqi.hotel.dto.LoginRequest;
import com.yueqi.hotel.dto.LoginResponse;
import com.yueqi.hotel.entity.User;
import com.yueqi.hotel.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final TokenService tokenService;

    @Value("${auth.token.admin-ttl-minutes:480}")
    private long adminTokenTtlMinutes;

    public AuthService(UserRepository userRepository, PasswordService passwordService, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.tokenService = tokenService;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误"));

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "账号已被禁用");
        }

        if (!passwordService.matches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }

        if (!passwordService.isHash(user.getPassword())) {
            user.setPassword(passwordService.hash(request.password()));
            userRepository.save(user);
        }

        String token = tokenService.issue(
                "admin",
                String.valueOf(user.getId()),
                Duration.ofMinutes(adminTokenTtlMinutes),
                java.util.Map.of("role", user.getRole()));

        return new LoginResponse(
                token,
                user.getUsername(),
                user.getRole(),
                user.getDisplayName());
    }

    public Optional<User> validateToken(String token) {
        Optional<TokenService.TokenPayload> payload = tokenService.verify(token, "admin");
        if (payload.isEmpty()) {
            return Optional.empty();
        }
        Long userId;
        try {
            userId = Long.valueOf(payload.get().subject());
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
        return userRepository.findById(userId)
                .filter(user -> Boolean.TRUE.equals(user.getEnabled()));
    }

    public void logout(String token) {
        tokenService.revoke(token);
    }
}
