package com.feusap.ecommerce.payment_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Creates payment.completed with several partitions. Without it, the topic is auto-created with a single
 * partition, and only one replica of a consumer group (e.g. one order-service out of two) can read it.
 * If the topic already exists with fewer partitions, Spring's KafkaAdmin adds the missing ones at startup.
 */
@Configuration
public class KafkaTopicConfig {

    private static final int PARTITIONS = 3;
    private static final int REPLICAS = 1;

    @Bean
    NewTopic paymentCompletedTopic(@Value("${app.kafka.topics.payment-completed}") String topicName) {
        return TopicBuilder.name(topicName).partitions(PARTITIONS).replicas(REPLICAS).build();
    }
}
