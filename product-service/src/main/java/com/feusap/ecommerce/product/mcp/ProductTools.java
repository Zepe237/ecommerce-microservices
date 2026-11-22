package com.feusap.ecommerce.product.mcp;

import com.feusap.ecommerce.product.dto.ProductResponse;
import com.feusap.ecommerce.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductTools {
    private final ProductService productService;
    @Bean
    ToolCallbackProvider productToolCallbacks(ProductTools productTools) {
        return MethodToolCallbackProvider.builder().toolObjects(productTools).build();
    }

    @Tool(description = """
        Get the details of one product of the catalog from its identifier: \
        name, description and price. \
        Use it when the customer mentions a specific product identifier, \
        or to show the details of a product found with another tool.""")
    public ProductResponse getProduct(
        @ToolParam(description = "The product identifier, for example PRD-0001") String productId) {
        return productService.findById(productId);
    }

    @Tool(description = """
        Search the catalog for products whose name or description contains a keyword, \
        optionally under a maximum price. Returns the matching products with their identifier, \
        name, description and price (an empty list when nothing matches). \
        Use it when the customer looks for a kind of product or a brand, with or without a budget. \
        Product names and descriptions are in English: translate the customer's words \
        into a short English keyword (for example "earbuds", "laptop", "iPhone").""")
    public List<ProductResponse> searchProducts(
        @ToolParam(description = "One English keyword to look for in product names and descriptions, for example earbuds")
        String query,
        @ToolParam(required = false, description = "Maximum price in Canadian dollars; leave empty when the customer gives no budget")
        BigDecimal maxPrice) {
        return productService.search(query, maxPrice);
    }
}
