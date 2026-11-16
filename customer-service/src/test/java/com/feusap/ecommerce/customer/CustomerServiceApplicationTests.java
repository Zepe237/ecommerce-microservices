package com.feusap.ecommerce.customer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CustomerServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
