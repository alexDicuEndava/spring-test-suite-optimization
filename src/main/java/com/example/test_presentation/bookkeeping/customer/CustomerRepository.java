package com.example.test_presentation.bookkeeping.customer;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

	List<Customer> findByStatus(CustomerStatus status);
}
