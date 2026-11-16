package com.feusap.ecommerce.customer.mapper;

import com.feusap.ecommerce.customer.domain.Customer;
import com.feusap.ecommerce.customer.dto.CustomerRequest;
import com.feusap.ecommerce.customer.dto.CustomerResponse;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public Customer toEntity(CustomerRequest request) {
        return new Customer(request.name(), normalizeEmail(request.email()));
    }

    public CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(customer.getId(), customer.getName(), customer.getEmail(), customer.getCreatedAt());
    }

    public String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
