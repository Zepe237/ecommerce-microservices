package com.feusap.ecommerce.order.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    private static final int PARTITIONS = 3;
    private static final int REPLICAS = 1;

    @Bean
    NewTopic orderConfirmedTopic(@Value("${app.kafka.topics.order-confirmed}") String topicName) {
        return TopicBuilder.name(topicName).partitions(PARTITIONS).replicas(REPLICAS).build();
    }

    @Bean
    NewTopic orderCancelledTopic(@Value("${app.kafka.topics.order-cancelled}") String topicName) {
        return TopicBuilder.name(topicName).partitions(PARTITIONS).replicas(REPLICAS).build();
    }
}
