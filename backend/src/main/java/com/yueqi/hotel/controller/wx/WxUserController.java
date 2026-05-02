package com.yueqi.hotel.controller.wx;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yueqi.hotel.common.ApiResponse;
import com.yueqi.hotel.service.ProfileService;
import com.yueqi.hotel.service.WxAuthService;

@RestController
@RequestMapping("/api/wx/users")
public class WxUserController {

    private final ProfileService profileService;
    private final WxAuthService wxAuthService;

    public WxUserController(ProfileService profileService, WxAuthService wxAuthService) {
        this.profileService = profileService;
        this.wxAuthService = wxAuthService;
    }

    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> profile(HttpServletRequest request) {
        return ApiResponse.success(profileService.getProfile(WxRequestAuth.requireSession(request, wxAuthService).openid()));
    }
}
