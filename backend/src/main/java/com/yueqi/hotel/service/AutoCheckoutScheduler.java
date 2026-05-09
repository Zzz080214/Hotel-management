package com.yueqi.hotel.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AutoCheckoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(AutoCheckoutScheduler.class);

    private final OrderService orderService;

    public AutoCheckoutScheduler(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(cron = "0 * * * * *", zone = "Asia/Shanghai")
    public void autoCheckoutOverdueOrders() {
        int count = orderService.autoCheckoutOverdueOrders(LocalDateTime.now());
        if (count > 0) {
            log.info("Auto checked out {} overdue staying order(s).", count);
        }
    }
}
