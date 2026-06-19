package com.example.test_presentation.bookkeeping.dto;

import com.example.test_presentation.bookkeeping.model.Customer;
import com.example.test_presentation.bookkeeping.model.CustomerStatus;

public record CustomerResponse(Long id, String name, String email, CustomerStatus status) {

	public static CustomerResponse from(Customer customer) {
		return new CustomerResponse(customer.getId(), customer.getName(), customer.getEmail(), customer.getStatus());
	}
}
