package com.example.test_presentation.bookkeeping.customer;

public record CustomerResponse(Long id, String name, String email, CustomerStatus status) {

	static CustomerResponse from(Customer customer) {
		return new CustomerResponse(customer.getId(), customer.getName(), customer.getEmail(), customer.getStatus());
	}
}
