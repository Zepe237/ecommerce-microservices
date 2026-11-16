package com.feusap.ecommerce.cart.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Configuration
public class KafkaConsumerConfig {

    private static final long RETRY_INTERVAL_MS = 5_000L;
    private static final long MAX_RETRIES = 3L;

    @Bean
    DefaultErrorHandler kafkaErrorHandler() {
        return new DefaultErrorHandler(
                (consumerRecord, ex) -> log.error("Giving up on record {} after {} retries",
                        consumerRecord.value(), MAX_RETRIES, ex),
                new FixedBackOff(RETRY_INTERVAL_MS, MAX_RETRIES));
    }
}
