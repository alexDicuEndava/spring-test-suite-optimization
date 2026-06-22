package com.example.test_presentation.bookkeeping.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.test_presentation.bookkeeping.model.Customer;
import com.example.test_presentation.bookkeeping.model.CustomerStatus;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
		"bookkeeping.demo.startup-delay-enabled=true",
		"bookkeeping.demo.startup-delay=2s",
		"spring.main.allow-bean-definition-overriding=true"
})
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CustomerRepositoryTest {

	@Autowired
	CustomerRepository customerRepository;

	@ParameterizedTest(name = "finds customers by {0}")
	@CsvSource({
			"ACTIVE,Acme",
			"INACTIVE,Globex",
			"ACTIVE,Northwind",
			"INACTIVE,Initech"
	})
	void findsCustomersByStatus(CustomerStatus status, String name) {
		customerRepository.save(new Customer(name, name.toLowerCase() + "@test.example", status));

		assertThat(customerRepository.findByStatus(status))
				.extracting(Customer::getName)
				.contains(name);
	}
}
