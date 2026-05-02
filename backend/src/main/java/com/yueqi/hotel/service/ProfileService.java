package com.yueqi.hotel.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yueqi.hotel.entity.UserProfile;
import com.yueqi.hotel.repository.UserProfileRepository;

@Service
public class ProfileService {

    private final UserProfileRepository userProfileRepository;
    private final OrderService orderService;

    public ProfileService(UserProfileRepository userProfileRepository, OrderService orderService) {
        this.userProfileRepository = userProfileRepository;
        this.orderService = orderService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getProfile(String wxOpenid) {
        UserProfile profile = userProfileRepository.findById(1L).orElseGet(this::defaultProfile);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("nickname", profile.getNickname());
        data.put("description", profile.getDescription());
        data.put("orderCount", orderService.countByWxOpenid(wxOpenid));
        data.put("couponCount", profile.getCouponCount());
        data.put("matchRate", profile.getMatchRate());
        return data;
    }

    private UserProfile defaultProfile() {
        UserProfile profile = new UserProfile();
        profile.setId(1L);
        profile.setNickname("酒店住客");
        profile.setDescription("欢迎再次入住悦栖酒店");
        profile.setCouponCount(2);
        profile.setMatchRate("93%");
        return profile;
    }
}
