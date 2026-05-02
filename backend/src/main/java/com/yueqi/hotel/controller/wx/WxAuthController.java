package com.yueqi.hotel.controller.wx;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yueqi.hotel.common.ApiResponse;
import com.yueqi.hotel.dto.WxLoginRequest;
import com.yueqi.hotel.dto.WxLoginResponse;
import com.yueqi.hotel.service.WxAuthService;
import com.yueqi.hotel.service.WxAuthService.WxSession;

@RestController
@RequestMapping("/api/wx/auth")
public class WxAuthController {

    private final WxAuthService wxAuthService;

    public WxAuthController(WxAuthService wxAuthService) {
        this.wxAuthService = wxAuthService;
    }

    @PostMapping("/login")
    public ApiResponse<WxLoginResponse> login(@Valid @RequestBody WxLoginRequest request) {
        return ApiResponse.success(wxAuthService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        WxRequestAuth.extractToken(request).ifPresent(wxAuthService::logout);
        return ApiResponse.success();
    }

    @GetMapping("/me")
    public ApiResponse<WxSession> currentUser(HttpServletRequest request) {
        return ApiResponse.success(WxRequestAuth.requireSession(request, wxAuthService));
    }
}
