package com.feusap.ecommerce.order.scheduler;

import com.feusap.ecommerce.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderExpirySchedulerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderExpiryScheduler scheduler;

    @Test
    @DisplayName("The cutoff is now minus the configured payment timeout")
    void cutoffIsNowMinusTimeout() {
        ReflectionTestUtils.setField(scheduler, "paymentTimeout", Duration.ofMinutes(15));

        scheduler.expireUnpaidOrders();

        ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
        verify(orderService).expireUnpaidOrders(cutoff.capture());
        Instant expected = Instant.now().minus(Duration.ofMinutes(15));
        assertThat(cutoff.getValue()).isCloseTo(expected, within(Duration.ofSeconds(5)));
    }
}
