package com.feusap.ecommerce.order.scheduler;

import com.feusap.ecommerce.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpiryScheduler {

    private final OrderService orderService;

    @Value("${app.saga.payment-timeout}")
    private Duration paymentTimeout;

    @Scheduled(fixedDelayString = "${app.saga.expiry-check-interval}")
    public void expireUnpaidOrders() {
        int count = orderService.expireUnpaidOrders(Instant.now().minus(paymentTimeout));
        if (count > 0) {
            log.info("{} unpaid order(s) expired", count);
        }
    }
}
