package com.feusap.ecommerce.product.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfig {
    @Bean
    ToolCallbackProvider productToolCallbacks(ProductTools productTools) {
        return MethodToolCallbackProvider.builder().toolObjects(productTools).build();
    }
}
