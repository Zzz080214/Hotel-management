package com.yueqi.hotel.controller.admin;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.yueqi.hotel.common.ApiResponse;
import com.yueqi.hotel.config.AuthInterceptor;
import com.yueqi.hotel.dto.UserUpsertRequest;
import com.yueqi.hotel.dto.UserView;
import com.yueqi.hotel.entity.User;
import com.yueqi.hotel.service.UserAdminService;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserAdminService userAdminService;

    public AdminUserController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public ApiResponse<List<UserView>> list(HttpServletRequest request) {
        requireAdmin(request);
        return ApiResponse.success(userAdminService.listAll());
    }

    @PostMapping
    public ApiResponse<UserView> create(@Valid @RequestBody UserUpsertRequest request,
                                        HttpServletRequest httpRequest) {
        requireAdmin(httpRequest);
        return ApiResponse.success(userAdminService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserView> update(@PathVariable Long id,
                                        @Valid @RequestBody UserUpsertRequest request,
                                        HttpServletRequest httpRequest) {
        User currentUser = requireAdmin(httpRequest);
        return ApiResponse.success(userAdminService.update(id, request, currentUser));
    }

    private User requireAdmin(HttpServletRequest request) {
        User currentUser = AuthInterceptor.currentUser(request);
        if (currentUser == null || !"ADMIN".equals(currentUser.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅系统管理员可管理账号与权限");
        }
        return currentUser;
    }
}
