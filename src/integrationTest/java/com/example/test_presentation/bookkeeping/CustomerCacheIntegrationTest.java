package com.example.test_presentation.bookkeeping;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.test_presentation.bookkeeping.customer.Customer;
import com.example.test_presentation.bookkeeping.customer.CustomerRepository;
import com.example.test_presentation.bookkeeping.customer.CustomerService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.jdbc.Sql;

@Sql(scripts = "/sql/bookkeeping-cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/customers-create.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/bookkeeping-cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class CustomerCacheIntegrationTest extends OptimizedIntegrationTest {

	@Autowired
	CustomerService customerService;

	@Autowired
	CustomerRepository customerRepository;

	@Autowired
	CacheManager cacheManager;

	@AfterEach
	void clearCaches() {
		cacheManager.getCacheNames().forEach(name -> {
			Cache cache = cacheManager.getCache(name);
			if (cache != null) {
				cache.clear();
			}
		});
	}

	@Test
	void clearsCacheExplicitlyInsteadOfDirtyingContext() {
		assertThat(customerService.find(100L).name()).isEqualTo("Acme Books");

		Customer customer = customerRepository.findById(100L).orElseThrow();
		customer.setName("Acme Books Updated Directly");
		customerRepository.saveAndFlush(customer);

		assertThat(customerService.find(100L).name()).isEqualTo("Acme Books");
		clearCaches();
		assertThat(customerService.find(100L).name()).isEqualTo("Acme Books Updated Directly");
	}
}
