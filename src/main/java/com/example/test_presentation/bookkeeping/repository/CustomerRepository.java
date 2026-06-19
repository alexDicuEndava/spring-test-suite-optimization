package com.example.test_presentation.bookkeeping.repository;

import java.util.List;

import com.example.test_presentation.bookkeeping.model.Customer;
import com.example.test_presentation.bookkeeping.model.CustomerStatus;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

	List<Customer> findByStatus(CustomerStatus status);
}
