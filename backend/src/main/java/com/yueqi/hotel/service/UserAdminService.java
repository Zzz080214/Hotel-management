package com.yueqi.hotel.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.yueqi.hotel.dto.UserUpsertRequest;
import com.yueqi.hotel.dto.UserView;
import com.yueqi.hotel.entity.User;
import com.yueqi.hotel.repository.UserRepository;

@Service
public class UserAdminService {

    private static final List<String> ALLOWED_ROLES = List.of("ADMIN", "MANAGER", "STAFF");

    private final UserRepository userRepository;
    private final PasswordService passwordService;

    public UserAdminService(UserRepository userRepository, PasswordService passwordService) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
    }

    @Transactional(readOnly = true)
    public List<UserView> listAll() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getId))
                .map(this::toView)
                .toList();
    }

    @Transactional
    public UserView create(UserUpsertRequest request) {
        String username = normalizeUsername(request.username());
        if (!StringUtils.hasText(request.password())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "新建用户必须设置初始密码");
        }
        userRepository.findByUsername(username).ifPresent(user -> {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "账号已存在");
        });

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordService.hash(normalizePassword(request.password())));
        user.setRole(normalizeRole(request.role()));
        user.setDisplayName(resolveDisplayName(request.displayName(), username, null));
        user.setEnabled(request.enabled() == null || request.enabled());
        return toView(userRepository.save(user));
    }

    @Transactional
    public UserView update(Long id, UserUpsertRequest request, User currentUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在"));

        String username = normalizeUsername(request.username());
        userRepository.findByUsername(username)
                .filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "账号已存在");
                });

        boolean isSelf = currentUser != null && currentUser.getId() != null && currentUser.getId().equals(user.getId());
        if (isSelf && Boolean.FALSE.equals(request.enabled())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能禁用当前登录的管理员账号");
        }
        if (isSelf && StringUtils.hasText(request.role()) && !"ADMIN".equals(normalizeRole(request.role()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能降低当前登录管理员的角色");
        }

        user.setUsername(username);
        if (StringUtils.hasText(request.password())) {
            user.setPassword(passwordService.hash(normalizePassword(request.password())));
        }
        user.setRole(normalizeRole(request.role()));
        user.setDisplayName(resolveDisplayName(request.displayName(), username, user.getDisplayName()));
        user.setEnabled(request.enabled() == null ? user.getEnabled() : request.enabled());
        return toView(userRepository.save(user));
    }

    private UserView toView(User user) {
        return new UserView(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getDisplayName(),
                user.getEnabled());
    }

    private String normalizeUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "账号不能为空");
        }
        return username.trim();
    }

    private String normalizeRole(String role) {
        if (!StringUtils.hasText(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "角色不能为空");
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_ROLES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "角色必须是 ADMIN、MANAGER 或 STAFF");
        }
        return normalized;
    }

    private String normalizePassword(String password) {
        if (!StringUtils.hasText(password)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码不能为空");
        }
        String normalized = password.trim();
        if (normalized.length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码长度不能少于 6 位");
        }
        return normalized;
    }

    private String resolveDisplayName(String displayName, String username, String currentDisplayName) {
        if (StringUtils.hasText(displayName)) {
            return displayName.trim();
        }
        if (StringUtils.hasText(currentDisplayName)) {
            return currentDisplayName;
        }
        return username;
    }
}
