package com.yueqi.hotel.controller.wx;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yueqi.hotel.common.ApiResponse;
import com.yueqi.hotel.dto.WxOrderCreateRequest;
import com.yueqi.hotel.entity.HotelOrder;
import com.yueqi.hotel.service.OrderService;
import com.yueqi.hotel.service.WxAuthService;
import com.yueqi.hotel.service.WxAuthService.WxSession;

@RestController
@RequestMapping("/api/wx/orders")
public class WxOrderController {

    private final OrderService orderService;
    private final WxAuthService wxAuthService;

    public WxOrderController(OrderService orderService, WxAuthService wxAuthService) {
        this.orderService = orderService;
        this.wxAuthService = wxAuthService;
    }

    @PostMapping
    public ApiResponse<HotelOrder> create(@Valid @RequestBody WxOrderCreateRequest request,
                                          HttpServletRequest httpRequest) {
        WxSession session = WxRequestAuth.requireSession(httpRequest, wxAuthService);
        return ApiResponse.success(orderService.createOrderForWx(request, session.openid()));
    }

    @GetMapping("/my")
    public ApiResponse<List<HotelOrder>> mine(@RequestParam(required = false) String status,
                                              HttpServletRequest request) {
        WxSession session = WxRequestAuth.requireSession(request, wxAuthService);
        return ApiResponse.success(orderService.listMyOrders(status, session.openid()));
    }

    @GetMapping("/{id}")
    public ApiResponse<HotelOrder> detail(@PathVariable String id,
                                          HttpServletRequest request) {
        WxSession session = WxRequestAuth.requireSession(request, wxAuthService);
        return ApiResponse.success(orderService.getRequiredForWx(id, session.openid()));
    }

    @PostMapping("/{id}/face-check-in")
    public ApiResponse<HotelOrder> faceCheckIn(@PathVariable String id,
                                               HttpServletRequest request) {
        WxSession session = WxRequestAuth.requireSession(request, wxAuthService);
        return ApiResponse.success(orderService.selfCheckInForWx(id, session.openid()));
    }

    @PostMapping("/{id}/pay")
    public ApiResponse<HotelOrder> pay(@PathVariable String id,
                                       HttpServletRequest request) {
        WxSession session = WxRequestAuth.requireSession(request, wxAuthService);
        return ApiResponse.success(orderService.payForWx(id, session.openid()));
    }

    @PostMapping("/{id}/self-check-out")
    public ApiResponse<HotelOrder> selfCheckOut(@PathVariable String id,
                                                HttpServletRequest request) {
        WxSession session = WxRequestAuth.requireSession(request, wxAuthService);
        return ApiResponse.success(orderService.selfCheckOutForWx(id, session.openid()));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<HotelOrder> cancel(@PathVariable String id,
                                          HttpServletRequest request) {
        WxSession session = WxRequestAuth.requireSession(request, wxAuthService);
        return ApiResponse.success(orderService.cancelForWx(id, session.openid()));
    }
}
