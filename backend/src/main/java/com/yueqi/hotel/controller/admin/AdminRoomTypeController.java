package com.yueqi.hotel.controller.admin;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yueqi.hotel.common.ApiResponse;
import com.yueqi.hotel.dto.RoomTypeRequest;
import com.yueqi.hotel.entity.RoomType;
import com.yueqi.hotel.service.RoomTypeService;

@RestController
@RequestMapping("/api/admin/room-types")
public class AdminRoomTypeController {

    private final RoomTypeService roomTypeService;

    public AdminRoomTypeController(RoomTypeService roomTypeService) {
        this.roomTypeService = roomTypeService;
    }

    @GetMapping
    public ApiResponse<List<RoomType>> list() {
        return ApiResponse.success(roomTypeService.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<RoomType> detail(@PathVariable Long id) {
        return ApiResponse.success(roomTypeService.getRequired(id));
    }

    @PostMapping
    public ApiResponse<RoomType> create(@Valid @RequestBody RoomTypeRequest request) {
        return ApiResponse.success(roomTypeService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<RoomType> update(@PathVariable Long id, @Valid @RequestBody RoomTypeRequest request) {
        return ApiResponse.success(roomTypeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roomTypeService.disable(id);
        return ApiResponse.success();
    }
}
