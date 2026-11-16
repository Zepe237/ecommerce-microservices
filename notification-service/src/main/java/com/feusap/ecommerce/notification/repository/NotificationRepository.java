package com.feusap.ecommerce.notification.repository;

import com.feusap.ecommerce.notification.domain.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    boolean existsByOrderId(Long orderId);

    List<Notification> findByCustomerIdOrderBySentAtDesc(Long customerId);
}
