package com.example.test_presentation.bookkeeping.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import com.example.test_presentation.bookkeeping.common.ResourceNotFoundException;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CustomerServiceUnitTest {

	private final CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
	private final CustomerService customerService = new CustomerService(customerRepository);

	@Test
	void createsCustomerWithoutSpringContext() {
		Customer saved = new Customer("Acme Books", "billing@acme.test", CustomerStatus.ACTIVE);
		given(customerRepository.save(Mockito.any(Customer.class))).willReturn(saved);

		CustomerResponse response = customerService.create(
				new CustomerRequest("Acme Books", "billing@acme.test", CustomerStatus.ACTIVE));

		assertThat(response.name()).isEqualTo("Acme Books");
		assertThat(response.status()).isEqualTo(CustomerStatus.ACTIVE);
	}

	@Test
	void filtersByStatusWithoutLoadingRepositoriesThroughSpring() {
		given(customerRepository.findByStatus(CustomerStatus.ACTIVE))
				.willReturn(List.of(new Customer("Northwind", "ar@northwind.test", CustomerStatus.ACTIVE)));

		List<CustomerResponse> customers = customerService.findAll(Optional.of(CustomerStatus.ACTIVE));

		assertThat(customers).extracting(CustomerResponse::name).containsExactly("Northwind");
	}

	@Test
	void updatesExistingCustomer() {
		Customer customer = new Customer("Old Name", "old@example.test", CustomerStatus.INACTIVE);
		given(customerRepository.findById(42L)).willReturn(Optional.of(customer));

		CustomerResponse response = customerService.update(42L,
				new CustomerRequest("New Name", "new@example.test", CustomerStatus.ACTIVE));

		assertThat(response.name()).isEqualTo("New Name");
		assertThat(response.email()).isEqualTo("new@example.test");
		assertThat(response.status()).isEqualTo(CustomerStatus.ACTIVE);
	}

	@Test
	void deleteFailsFastWhenCustomerDoesNotExist() {
		given(customerRepository.existsById(99L)).willReturn(false);

		assertThatThrownBy(() -> customerService.delete(99L))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Customer 99");
	}

	@Test
	void deletesExistingCustomer() {
		given(customerRepository.existsById(10L)).willReturn(true);

		customerService.delete(10L);

		verify(customerRepository).deleteById(10L);
	}
}
