package com.yueqi.hotel.service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.yueqi.hotel.dto.LoginRequest;
import com.yueqi.hotel.dto.LoginResponse;
import com.yueqi.hotel.entity.User;
import com.yueqi.hotel.repository.UserRepository;

@Service
public class AuthService {

    /** token → user 的内存存储（演示环境，生产应换 Redis/JWT） */
    private final Map<String, User> tokenStore = new ConcurrentHashMap<>();

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误"));

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "账号已被禁用");
        }

        if (!user.getPassword().equals(request.password())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        tokenStore.put(token, user);

        return new LoginResponse(
                token,
                user.getUsername(),
                user.getRole(),
                user.getDisplayName());
    }

    public Optional<User> validateToken(String token) {
        return Optional.ofNullable(tokenStore.get(token));
    }

    public void logout(String token) {
        tokenStore.remove(token);
    }
}
