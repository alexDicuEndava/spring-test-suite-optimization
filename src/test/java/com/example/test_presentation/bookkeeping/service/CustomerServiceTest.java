package com.example.test_presentation.bookkeeping.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.example.test_presentation.bookkeeping.dto.CustomerInvoiceSummaryResponse;
import com.example.test_presentation.bookkeeping.dto.CustomerRequest;
import com.example.test_presentation.bookkeeping.dto.CustomerResponse;
import com.example.test_presentation.bookkeeping.exception.ResourceNotFoundException;
import com.example.test_presentation.bookkeeping.model.Customer;
import com.example.test_presentation.bookkeeping.model.CustomerStatus;
import com.example.test_presentation.bookkeeping.model.Invoice;
import com.example.test_presentation.bookkeeping.model.InvoiceStatus;
import com.example.test_presentation.bookkeeping.repository.CustomerRepository;
import com.example.test_presentation.bookkeeping.repository.InvoiceRepository;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CustomerServiceTest {

	private final CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
	private final InvoiceRepository invoiceRepository = Mockito.mock(InvoiceRepository.class);
	private final CustomerService customerService = new CustomerService(customerRepository, invoiceRepository);

	@ParameterizedTest(name = "creates customer {0}")
	@CsvSource({
			"Acme Books,billing@acme.test,ACTIVE",
			"Northwind,ar@northwind.test,ACTIVE",
			"Globex,ap@globex.test,INACTIVE",
			"Umbrella,pay@umbrella.test,ACTIVE",
			"Initech,office@initech.test,INACTIVE",
			"Stark,finance@stark.test,ACTIVE",
			"Wayne,ledger@wayne.test,ACTIVE",
			"Wonka,accounts@wonka.test,INACTIVE",
			"Soylent,billing@soylent.test,ACTIVE",
			"Tyrell,receipts@tyrell.test,INACTIVE"
	})
	void createsCustomer(String name, String email, CustomerStatus status) {
		given(customerRepository.save(Mockito.any(Customer.class)))
				.willAnswer((invocation) -> invocation.getArgument(0));

		CustomerResponse response = customerService.create(new CustomerRequest(name, email, status));

		assertThat(response.name()).isEqualTo(name);
		assertThat(response.email()).isEqualTo(email);
		assertThat(response.status()).isEqualTo(status);
	}

	@ParameterizedTest(name = "lists customers for {0}")
	@MethodSource("listCases")
	void listsCustomers(Optional<CustomerStatus> filter, List<Customer> stored, List<String> expectedNames) {
		if (filter.isPresent()) {
			given(customerRepository.findByStatus(filter.get())).willReturn(stored);
		}
		else {
			given(customerRepository.findAll()).willReturn(stored);
		}

		List<CustomerResponse> customers = customerService.findAll(filter);

		assertThat(customers).extracting(CustomerResponse::name).containsExactlyElementsOf(expectedNames);
	}

	static Stream<Arguments> listCases() {
		return Stream.of(
				Arguments.of(Optional.empty(), List.of(customer("Acme", CustomerStatus.ACTIVE)), List.of("Acme")),
				Arguments.of(Optional.of(CustomerStatus.ACTIVE), List.of(customer("Northwind", CustomerStatus.ACTIVE)),
						List.of("Northwind")),
				Arguments.of(Optional.of(CustomerStatus.INACTIVE), List.of(customer("Globex", CustomerStatus.INACTIVE)),
						List.of("Globex")),
				Arguments.of(Optional.of(CustomerStatus.ACTIVE), List.of(), List.of()));
	}

	@ParameterizedTest(name = "updates customer {0}")
	@CsvSource({
			"1,New Acme,new-acme@test.example,ACTIVE",
			"2,New Globex,new-globex@test.example,INACTIVE",
			"3,New Initech,new-initech@test.example,ACTIVE",
			"4,New Wayne,new-wayne@test.example,ACTIVE",
			"5,New Stark,new-stark@test.example,INACTIVE",
			"6,New Wonka,new-wonka@test.example,ACTIVE",
			"7,New Tyrell,new-tyrell@test.example,INACTIVE",
			"8,New Umbrella,new-umbrella@test.example,ACTIVE"
	})
	void updatesCustomer(long id, String name, String email, CustomerStatus status) {
		Customer customer = customer("Old", CustomerStatus.INACTIVE);
		given(customerRepository.findById(id)).willReturn(Optional.of(customer));

		CustomerResponse response = customerService.update(id, new CustomerRequest(name, email, status));

		assertThat(response.name()).isEqualTo(name);
		assertThat(response.email()).isEqualTo(email);
		assertThat(response.status()).isEqualTo(status);
	}

	@ParameterizedTest(name = "missing find {0}")
	@CsvSource({ "41", "42", "43" })
	void findMissingCustomerThrows(long id) {
		given(customerRepository.findById(id)).willReturn(Optional.empty());

		assertThatThrownBy(() -> customerService.find(id))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Customer " + id);
	}

	@ParameterizedTest(name = "deletes existing customer {0}")
	@CsvSource({ "10", "11", "12" })
	void deletesExistingCustomer(long id) {
		given(customerRepository.existsById(id)).willReturn(true);

		customerService.delete(id);

		verify(customerRepository).deleteById(id);
	}

	@ParameterizedTest(name = "missing delete {0}")
	@CsvSource({ "90", "91" })
	void deleteMissingCustomerThrows(long id) {
		given(customerRepository.existsById(id)).willReturn(false);

		assertThatThrownBy(() -> customerService.delete(id))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Customer " + id);
	}

	@ParameterizedTest(name = "summarizes invoices {0}")
	@MethodSource("summaryCases")
	void summarizesCustomerInvoices(long customerId, List<Invoice> invoices, String total, String open, String overdue) {
		given(customerRepository.existsById(customerId)).willReturn(true);
		given(invoiceRepository.findByCustomerId(customerId)).willReturn(invoices);

		CustomerInvoiceSummaryResponse response = customerService.invoiceSummary(customerId,
				Optional.of(LocalDate.parse("2026-03-01")));

		assertThat(response.invoiceCount()).isEqualTo(invoices.size());
		assertThat(response.totalInvoiced()).isEqualByComparingTo(total);
		assertThat(response.openBalance()).isEqualByComparingTo(open);
		assertThat(response.overdueBalance()).isEqualByComparingTo(overdue);
	}

	static Stream<Arguments> summaryCases() {
		return Stream.of(
				Arguments.of(1L, List.of(invoice("S-1", InvoiceStatus.SENT, "2026-02-01", "100.00")), "100.00",
						"100.00", "100.00"),
				Arguments.of(2L, List.of(invoice("S-2", InvoiceStatus.DRAFT, "2026-03-20", "45.00")), "45.00",
						"45.00", "0.00"),
				Arguments.of(3L, List.of(invoice("S-3", InvoiceStatus.PAID, "2026-02-01", "88.00")), "88.00",
						"0.00", "0.00"),
				Arguments.of(4L, List.of(invoice("S-4", InvoiceStatus.VOID, "2026-02-01", "12.00")), "12.00",
						"0.00", "0.00"),
				Arguments.of(5L, List.of(
						invoice("S-5A", InvoiceStatus.SENT, "2026-02-01", "10.00"),
						invoice("S-5B", InvoiceStatus.PAID, "2026-02-01", "20.00")), "30.00", "10.00", "10.00"),
				Arguments.of(6L, List.of(
						invoice("S-6A", InvoiceStatus.DRAFT, "2026-04-01", "30.00"),
						invoice("S-6B", InvoiceStatus.SENT, "2026-02-15", "40.00")), "70.00", "70.00", "40.00"),
				Arguments.of(7L, List.of(), "0.00", "0.00", "0.00"),
				Arguments.of(8L, List.of(
						invoice("S-8A", InvoiceStatus.SENT, "2026-02-01", "5.50"),
						invoice("S-8B", InvoiceStatus.SENT, "2026-02-20", "6.50"),
						invoice("S-8C", InvoiceStatus.PAID, "2026-01-01", "7.00")), "19.00", "12.00", "12.00"),
				Arguments.of(9L, List.of(invoice("S-9", InvoiceStatus.DRAFT, "2026-03-01", "15.00")), "15.00",
						"15.00", "0.00"),
				Arguments.of(10L, List.of(invoice("S-10", InvoiceStatus.SENT, "2026-02-28", "99.99")), "99.99",
						"99.99", "99.99"));
	}

	@ParameterizedTest(name = "missing summary customer {0}")
	@CsvSource({ "301", "302", "303", "304" })
	void invoiceSummaryMissingCustomerThrows(long customerId) {
		given(customerRepository.existsById(customerId)).willReturn(false);

		assertThatThrownBy(() -> customerService.invoiceSummary(customerId, Optional.of(LocalDate.parse("2026-03-01"))))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Customer " + customerId);
	}

	private static Customer customer(String name, CustomerStatus status) {
		return new Customer(name, name.toLowerCase().replace(" ", "-") + "@test.example", status);
	}

	private static Invoice invoice(String number, InvoiceStatus status, String dueDate, String total) {
		return new Invoice(number, customer("Acme", CustomerStatus.ACTIVE), status, LocalDate.parse("2026-01-01"),
				LocalDate.parse(dueDate), new BigDecimal(total));
	}
}
