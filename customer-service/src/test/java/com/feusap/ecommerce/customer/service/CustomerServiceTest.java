package com.feusap.ecommerce.customer.service;

import com.feusap.ecommerce.customer.domain.Customer;
import com.feusap.ecommerce.customer.dto.CustomerRequest;
import com.feusap.ecommerce.customer.dto.CustomerResponse;
import com.feusap.ecommerce.customer.exception.CustomerNotFoundException;
import com.feusap.ecommerce.customer.exception.EmailAlreadyUsedException;
import com.feusap.ecommerce.customer.mapper.CustomerMapper;
import com.feusap.ecommerce.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerService(customerRepository, new CustomerMapper());
    }

    @Test
    @DisplayName("create normalizes the e-mail address before saving")
    void create_normalizesEmail() {
        when(customerRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerResponse created = customerService.create(new CustomerRequest("Jane Doe", "  Jane@Example.com "));

        assertThat(created.email()).isEqualTo("jane@example.com");
    }

    @Test
    @DisplayName("create rejects an e-mail address that is already used")
    void create_rejectsDuplicateEmail() {
        when(customerRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.create(new CustomerRequest("Jane Doe", "jane@example.com")))
                .isInstanceOf(EmailAlreadyUsedException.class);
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("findById throws when the customer does not exist")
    void findById_throwsWhenMissing() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findById(99L)).isInstanceOf(CustomerNotFoundException.class);
    }
}
