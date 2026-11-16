package com.feusap.ecommerce.notification.controller;

import com.feusap.ecommerce.notification.dto.NotificationResponse;
import com.feusap.ecommerce.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "History of e-mails sent to customers")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "List the notifications sent to a customer, most recent first")
    public List<NotificationResponse> findByCustomer(@RequestParam Long customerId) {
        return notificationService.findByCustomer(customerId);
    }
}
