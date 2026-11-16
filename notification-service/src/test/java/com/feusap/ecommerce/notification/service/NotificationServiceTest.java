package com.feusap.ecommerce.notification.service;

import com.feusap.ecommerce.notification.client.CustomerClient;
import com.feusap.ecommerce.notification.client.dto.CustomerResponse;
import com.feusap.ecommerce.notification.config.MailProperties;
import com.feusap.ecommerce.notification.domain.Notification;
import com.feusap.ecommerce.notification.messaging.OrderCreatedEvent;
import com.feusap.ecommerce.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final OrderCreatedEvent EVENT = new OrderCreatedEvent(
            42L, 1L, new BigDecimal("2300.00"), Instant.now(),
            List.of(new OrderCreatedEvent.OrderedItem("PRD-0001", "iPhone 15", 2)));

    @Mock
    private CustomerClient customerClient;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(customerClient, mailSender, notificationRepository,
                new OrderConfirmationTemplate(), new MailProperties("no-reply@test.local"));
    }

    @Test
    @DisplayName("sendOrderConfirmation e-mails the customer and records the notification")
    void sendOrderConfirmation_sendsAndRecords() {
        when(notificationRepository.existsByOrderId(42L)).thenReturn(false);
        when(customerClient.getCustomer(1L)).thenReturn(new CustomerResponse(1L, "Jane Doe", "jane@example.com"));

        notificationService.sendOrderConfirmation(EVENT);

        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(message.capture());
        assertThat(message.getValue().getTo()).containsExactly("jane@example.com");
        assertThat(message.getValue().getText()).contains("iPhone 15 x 2").contains("2300.00");
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("sendOrderConfirmation does nothing for an order already notified")
    void sendOrderConfirmation_isIdempotent() {
        when(notificationRepository.existsByOrderId(42L)).thenReturn(true);

        notificationService.sendOrderConfirmation(EVENT);

        verifyNoInteractions(customerClient, mailSender);
    }
}
