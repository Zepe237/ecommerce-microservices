package com.feusap.ecommerce.notification.service;

import com.feusap.ecommerce.notification.client.CustomerClient;
import com.feusap.ecommerce.notification.client.dto.CustomerResponse;
import com.feusap.ecommerce.notification.config.MailProperties;
import com.feusap.ecommerce.notification.domain.Notification;
import com.feusap.ecommerce.notification.dto.NotificationResponse;
import com.feusap.ecommerce.notification.messaging.OrderCreatedEvent;
import com.feusap.ecommerce.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final CustomerClient customerClient;
    private final JavaMailSender mailSender;
    private final NotificationRepository notificationRepository;
    private final OrderConfirmationTemplate template;
    private final MailProperties mailProperties;

    /**
     * Sends the confirmation e-mail of an order. Idempotent: Kafka may redeliver a record,
     * so an order that already has a notification is skipped. Any failure propagates to the
     * Kafka error handler, which retries the record.
     */
    public void sendOrderConfirmation(OrderCreatedEvent event) {
        if (notificationRepository.existsByOrderId(event.orderId())) {
            log.info("Confirmation already sent for order {}, skipping", event.orderId());
            return;
        }

        CustomerResponse customer = customerClient.getCustomer(event.customerId());
        String subject = template.subject(event);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.from());
        message.setTo(customer.email());
        message.setSubject(subject);
        message.setText(template.body(customer.name(), event));
        mailSender.send(message);

        notificationRepository.save(
                Notification.orderConfirmation(event.orderId(), event.customerId(), customer.email(), subject));
        log.info("Confirmation of order {} sent to {}", event.orderId(), customer.email());
    }

    public List<NotificationResponse> findByCustomer(Long customerId) {
        return notificationRepository.findByCustomerIdOrderBySentAtDesc(customerId).stream()
                .map(n -> new NotificationResponse(n.orderId(), n.customerId(), n.recipient(), n.subject(), n.sentAt()))
                .toList();
    }
}
