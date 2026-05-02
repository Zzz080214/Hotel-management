package com.yueqi.hotel.controller.wx;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yueqi.hotel.common.ApiResponse;
import com.yueqi.hotel.entity.RoomType;
import com.yueqi.hotel.service.RoomTypeService;

@RestController
@RequestMapping("/api/wx/rooms")
public class WxRoomController {

    private final RoomTypeService roomTypeService;

    public WxRoomController(RoomTypeService roomTypeService) {
        this.roomTypeService = roomTypeService;
    }

    @GetMapping("/recommend")
    public ApiResponse<List<RoomType>> recommend(@RequestParam(defaultValue = "3") int limit) {
        return ApiResponse.success(roomTypeService.recommend(limit));
    }

    @GetMapping
    public ApiResponse<List<RoomType>> list() {
        return ApiResponse.success(roomTypeService.listEnabled());
    }

    @GetMapping("/{id}")
    public ApiResponse<RoomType> detail(@PathVariable Long id) {
        return ApiResponse.success(roomTypeService.getEnabled(id));
    }
}
