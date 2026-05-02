package com.yueqi.hotel.controller.admin;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yueqi.hotel.common.ApiResponse;
import com.yueqi.hotel.dto.CheckInRequest;
import com.yueqi.hotel.dto.WxOrderCreateRequest;
import com.yueqi.hotel.entity.HotelOrder;
import com.yueqi.hotel.service.OrderService;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<List<HotelOrder>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(orderService.adminList(keyword, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<HotelOrder> detail(@PathVariable String id) {
        return ApiResponse.success(orderService.getRequired(id));
    }

    @PostMapping
    public ApiResponse<HotelOrder> create(@Valid @RequestBody WxOrderCreateRequest request) {
        return ApiResponse.success(orderService.createOrder(request));
    }

    @PostMapping("/{id}/check-in")
    public ApiResponse<HotelOrder> checkIn(@PathVariable String id, @Valid @RequestBody CheckInRequest request) {
        return ApiResponse.success(orderService.checkIn(id, request));
    }

    @PostMapping("/{id}/check-out")
    public ApiResponse<HotelOrder> checkOut(@PathVariable String id) {
        return ApiResponse.success(orderService.checkOut(id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<HotelOrder> cancel(@PathVariable String id) {
        return ApiResponse.success(orderService.cancel(id));
    }
}
