package com.example.test_presentation.bookkeeping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import com.example.test_presentation.bookkeeping.common.ResourceNotFoundException;
import com.example.test_presentation.bookkeeping.customer.Customer;
import com.example.test_presentation.bookkeeping.customer.CustomerRequest;
import com.example.test_presentation.bookkeeping.customer.CustomerResponse;
import com.example.test_presentation.bookkeeping.customer.CustomerRepository;
import com.example.test_presentation.bookkeeping.customer.CustomerService;
import com.example.test_presentation.bookkeeping.customer.CustomerStatus;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
		BadSuiteProperties.STARTUP_DELAY_ENABLED,
		BadSuiteProperties.STARTUP_DELAY,
		"bookkeeping.bad-suite.case=customer-service"
})
class CustomerServiceBadSuiteTest {

	@Autowired
	CustomerService customerService;

	@MockitoBean
	CustomerRepository customerRepository;

	@Test
	void createsCustomerThroughFullSpringContextAndMockBean() {
		Customer saved = new Customer("Acme Books", "billing@acme.test", CustomerStatus.ACTIVE);
		given(customerRepository.save(Mockito.any(Customer.class))).willReturn(saved);

		CustomerResponse response = customerService.create(
				new CustomerRequest("Acme Books", "billing@acme.test", CustomerStatus.ACTIVE));

		assertThat(response.name()).isEqualTo("Acme Books");
		assertThat(response.status()).isEqualTo(CustomerStatus.ACTIVE);
	}

	@Test
	void filtersCustomersThroughFullSpringContextAndMockBean() {
		given(customerRepository.findByStatus(CustomerStatus.ACTIVE))
				.willReturn(List.of(new Customer("Northwind", "ar@northwind.test", CustomerStatus.ACTIVE)));

		assertThat(customerService.findAll(Optional.of(CustomerStatus.ACTIVE)))
				.extracting(CustomerResponse::name)
				.containsExactly("Northwind");
	}

	@Test
	void updatesCustomerThroughFullSpringContextAndMockBean() {
		Customer customer = new Customer("Old Name", "old@example.test", CustomerStatus.INACTIVE);
		given(customerRepository.findById(42L)).willReturn(Optional.of(customer));

		CustomerResponse response = customerService.update(42L,
				new CustomerRequest("New Name", "new@example.test", CustomerStatus.ACTIVE));

		assertThat(response.name()).isEqualTo("New Name");
		assertThat(response.email()).isEqualTo("new@example.test");
		assertThat(response.status()).isEqualTo(CustomerStatus.ACTIVE);
	}

	@Test
	void deleteFailsFastThroughFullSpringContextAndMockBean() {
		given(customerRepository.existsById(99L)).willReturn(false);

		assertThatThrownBy(() -> customerService.delete(99L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Customer 99");
	}

	@Test
	void deletesExistingCustomerThroughFullSpringContextAndMockBean() {
		given(customerRepository.existsById(10L)).willReturn(true);

		customerService.delete(10L);

		verify(customerRepository).deleteById(10L);
	}
}
