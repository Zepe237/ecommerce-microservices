package com.feusap.ecommerce.product.controller;

import com.feusap.ecommerce.product.dto.ProductResponse;
import com.feusap.ecommerce.product.exception.ProductNotFoundException;
import com.feusap.ecommerce.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer slice: only the MVC infrastructure is loaded, the service is mocked.
 */
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    @DisplayName("GET /api/products returns the catalog as JSON")
    void findAll_returnsCatalog() throws Exception {
        when(productService.findAll()).thenReturn(List.of(
                new ProductResponse("PRD-0001", "iPhone 15", "Apple smartphone", new BigDecimal("1099.99")),
                new ProductResponse("PRD-0002", "Galaxy S24", "Samsung smartphone", new BigDecimal("999.99"))
        ));

        mockMvc.perform(get("/api/products").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value("PRD-0001"))
                .andExpect(jsonPath("$[1].name").value("Galaxy S24"));
    }

    @Test
    @DisplayName("GET /api/products/{id} returns 404 as a problem detail when the product is unknown")
    void findById_returns404WhenMissing() throws Exception {
        when(productService.findById("unknown")).thenThrow(new ProductNotFoundException("unknown"));

        mockMvc.perform(get("/api/products/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Product not found: unknown"));
    }

    @Test
    @DisplayName("POST /api/products rejects an invalid payload with 400")
    void create_rejectsInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "price": -1}
                                """))
                .andExpect(status().isBadRequest());
    }
}
