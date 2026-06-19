package com.example.test_presentation.bookkeeping;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.test_presentation.bookkeeping.customer.Customer;
import com.example.test_presentation.bookkeeping.customer.CustomerRepository;
import com.example.test_presentation.bookkeeping.customer.CustomerService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest(properties = {
		BadSuiteProperties.STARTUP_DELAY_ENABLED,
		BadSuiteProperties.STARTUP_DELAY,
		"bookkeeping.bad-suite.case=cache-dirties-context"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(scripts = "/sql/bookkeeping-cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/customers-create.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/bookkeeping-cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class CustomerCacheWithDirtiesContextBadSuiteTest {

	@Autowired
	CustomerService customerService;

	@Autowired
	CustomerRepository customerRepository;

	@Test
	void usesDirtiesContextToRecoverFromCacheState() {
		assertThat(customerService.find(100L).name()).isEqualTo("Acme Books");

		Customer customer = customerRepository.findById(100L).orElseThrow();
		customer.setName("Acme Books Updated Directly");
		customerRepository.saveAndFlush(customer);

		assertThat(customerService.find(100L).name()).isEqualTo("Acme Books");
	}

	@Test
	void paysAnotherContextStartupCostForTheNextCacheSensitiveTest() {
		assertThat(customerService.find(101L).name()).isEqualTo("Dormant Ledger");
	}
}
