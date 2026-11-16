package com.feusap.ecommerce.product;

import com.feusap.ecommerce.product.domain.Product;
import com.feusap.ecommerce.product.repository.ProductRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.notNullValue;

/**
 * End-to-end test: real HTTP server, real MongoDB started by Testcontainers.
 * Requires a running Docker daemon.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class ProductApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        productRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/products returns the products stored in MongoDB")
    void findAll_returnsStoredProducts() {
        productRepository.saveAll(List.of(
                new Product(null, "Google Pixel 9", "Google smartphone", new BigDecimal("1199.00")),
                new Product(null, "Kindle", "Amazon e-reader", new BigDecimal("159.99"))
        ));

        given()
                .accept(ContentType.JSON)
        .when()
                .get("/api/products")
        .then()
                .statusCode(200)
                .body("size()", equalTo(2))
                .body("name", hasItems("Google Pixel 9", "Kindle"))
                .body("[0].id", notNullValue());
    }

    @Test
    @DisplayName("POST /api/products creates a product and returns its location")
    void create_returnsCreatedProduct() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "Kindle", "description": "Amazon e-reader", "price": 159.99}
                        """)
        .when()
                .post("/api/products")
        .then()
                .statusCode(201)
                .header("Location", notNullValue())
                .body("id", notNullValue())
                .body("name", equalTo("Kindle"));
    }

    @Test
    @DisplayName("GET /api/products/{id} returns 404 for an unknown product")
    void findById_returns404WhenMissing() {
        given()
        .when()
                .get("/api/products/{id}", "unknown")
        .then()
                .statusCode(404)
                .body("status", equalTo(404));
    }
}
