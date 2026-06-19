package com.example.test_presentation.bookkeeping.customer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class CustomerRepositorySliceTest {

	@Autowired
	CustomerRepository customerRepository;

	@Test
	void findsCustomersByStatusWithTransactionalRollback() {
		customerRepository.save(new Customer("Acme", "billing@acme.test", CustomerStatus.ACTIVE));
		customerRepository.save(new Customer("Dormant", "ap@dormant.test", CustomerStatus.INACTIVE));

		assertThat(customerRepository.findByStatus(CustomerStatus.ACTIVE))
				.extracting(Customer::getName)
				.containsExactly("Acme");
	}
}
