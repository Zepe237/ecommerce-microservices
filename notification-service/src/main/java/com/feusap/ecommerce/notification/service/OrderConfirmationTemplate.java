package com.feusap.ecommerce.notification.service;

import com.feusap.ecommerce.notification.messaging.OrderStatusChangedEvent;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Builds the plain-text order confirmation e-mail.
 */
@Component
public class OrderConfirmationTemplate {

    public String subject(OrderStatusChangedEvent event) {
        return "Confirmation de votre commande n° " + event.orderId();
    }

    public String body(String customerName, OrderStatusChangedEvent event) {
        String lines = event.items().stream()
                .map(item -> "  - %s x %d".formatted(item.productName(), item.quantity()))
                .collect(Collectors.joining("\n"));

        return """
                Bonjour %s,

                Votre commande n° %d a bien été enregistrée.

                Articles :
                %s

                Montant total : %s $

                Merci pour votre achat.
                """.formatted(customerName, event.orderId(), lines, event.totalAmount());
    }
}
