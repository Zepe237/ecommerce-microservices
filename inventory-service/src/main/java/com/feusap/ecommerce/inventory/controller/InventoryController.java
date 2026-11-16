package com.feusap.ecommerce.inventory.controller;

import com.feusap.ecommerce.inventory.dto.AvailabilityResponse;
import com.feusap.ecommerce.inventory.dto.StockItemRequest;
import com.feusap.ecommerce.inventory.dto.StockLevelResponse;
import com.feusap.ecommerce.inventory.dto.StockUpdateRequest;
import com.feusap.ecommerce.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Stock levels and availability")
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/availability")
    @Operation(summary = "Check the availability of a list of products",
            description = "Called by the order service before an order is placed.")
    public AvailabilityResponse checkAvailability(@RequestBody @NotEmpty List<@Valid StockItemRequest> items) {
        return inventoryService.checkAvailability(items);
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get the stock level of a product")
    public StockLevelResponse getStockLevel(@PathVariable String productId) {
        return inventoryService.getStockLevel(productId);
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Set the stock level of a product",
            description = "Creates the inventory line when it does not exist yet.")
    public StockLevelResponse setStock(@PathVariable String productId,
                                       @Valid @RequestBody StockUpdateRequest request) {
        return inventoryService.setStock(productId, request.quantity());
    }
}
