package com.feusap.ecommerce.customer.service;

import com.feusap.ecommerce.customer.domain.Customer;
import com.feusap.ecommerce.customer.dto.CustomerRequest;
import com.feusap.ecommerce.customer.dto.CustomerResponse;
import com.feusap.ecommerce.customer.exception.CustomerNotFoundException;
import com.feusap.ecommerce.customer.exception.EmailAlreadyUsedException;
import com.feusap.ecommerce.customer.mapper.CustomerMapper;
import com.feusap.ecommerce.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        String email = customerMapper.normalizeEmail(request.email());
        if (customerRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException(email);
        }
        Customer saved = customerRepository.save(customerMapper.toEntity(request));
        log.info("Customer {} created", saved.getId());
        return customerMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CustomerResponse findById(Long customerId) {
        return customerMapper.toResponse(loadCustomer(customerId));
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> findAll() {
        return customerRepository.findAll().stream()
                .map(customerMapper::toResponse)
                .toList();
    }

    @Transactional
    public CustomerResponse update(Long customerId, CustomerRequest request) {
        Customer customer = loadCustomer(customerId);
        String email = customerMapper.normalizeEmail(request.email());
        if (!customer.getEmail().equals(email) && customerRepository.existsByEmail(email)) {
            throw new EmailAlreadyUsedException(email);
        }
        customer.update(request.name(), email);
        log.info("Customer {} updated", customerId);
        return customerMapper.toResponse(customer);
    }

    @Transactional
    public void delete(Long customerId) {
        customerRepository.delete(loadCustomer(customerId));
        log.info("Customer {} deleted", customerId);
    }

    private Customer loadCustomer(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }
}
