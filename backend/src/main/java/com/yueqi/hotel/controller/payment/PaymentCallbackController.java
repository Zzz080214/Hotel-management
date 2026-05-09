package com.yueqi.hotel.controller.payment;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.yueqi.hotel.common.ApiResponse;
import com.yueqi.hotel.dto.PaymentCallbackRequest;
import com.yueqi.hotel.entity.HotelOrder;
import com.yueqi.hotel.service.OrderService;

@RestController
@RequestMapping("/api/payments/wechat")
public class PaymentCallbackController {

    private final OrderService orderService;

    @Value("${payment.callback-secret:local-payment-callback-secret}")
    private String callbackSecret;

    public PaymentCallbackController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/callback")
    public ApiResponse<HotelOrder> callback(
            @RequestHeader(value = "X-Payment-Callback-Secret", required = false) String providedSecret,
            @Valid @RequestBody PaymentCallbackRequest request) {
        if (!StringUtils.hasText(callbackSecret) || !callbackSecret.equals(providedSecret)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "支付回调签名无效");
        }
        return ApiResponse.success(orderService.confirmPaymentCallback(request));
    }
}
