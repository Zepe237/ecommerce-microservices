package com.feusap.ecommerce.customer.controller;

import com.feusap.ecommerce.customer.dto.CustomerRequest;
import com.feusap.ecommerce.customer.dto.CustomerResponse;
import com.feusap.ecommerce.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Customer profiles")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    @Operation(summary = "Create a customer")
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        CustomerResponse created = customerService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{customerId}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{customerId}")
    @Operation(summary = "Get a customer", description = "Also used by the notification service to resolve e-mail addresses.")
    public CustomerResponse findById(@PathVariable Long customerId) {
        return customerService.findById(customerId);
    }

    @GetMapping
    @Operation(summary = "List all customers")
    public List<CustomerResponse> findAll() {
        return customerService.findAll();
    }

    @PutMapping("/{customerId}")
    @Operation(summary = "Update a customer")
    public CustomerResponse update(@PathVariable Long customerId, @Valid @RequestBody CustomerRequest request) {
        return customerService.update(customerId, request);
    }

    @DeleteMapping("/{customerId}")
    @Operation(summary = "Delete a customer")
    public ResponseEntity<Void> delete(@PathVariable Long customerId) {
        customerService.delete(customerId);
        return ResponseEntity.noContent().build();
    }
}
