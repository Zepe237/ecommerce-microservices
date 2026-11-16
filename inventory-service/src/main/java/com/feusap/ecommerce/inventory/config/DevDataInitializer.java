package com.feusap.ecommerce.inventory.config;

import com.feusap.ecommerce.inventory.domain.InventoryItem;
import com.feusap.ecommerce.inventory.repository.InventoryItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

/**
 * Seeds stock levels matching the products seeded by the product service in development.
 */
@Slf4j
@Configuration
@Profile("dev")
public class DevDataInitializer {

    @Bean
    ApplicationRunner seedStock(InventoryItemRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                log.info("Inventory already populated, skipping seed");
                return;
            }
            List<InventoryItem> items = List.of(
                    new InventoryItem("PRD-0001", 25),
                    new InventoryItem("PRD-0002", 30),
                    new InventoryItem("PRD-0003", 10),
                    new InventoryItem("PRD-0004", 50),
                    new InventoryItem("PRD-0005", 40)
            );
            repository.saveAll(items);
            log.info("Seeded stock for {} products", items.size());
        };
    }
}
