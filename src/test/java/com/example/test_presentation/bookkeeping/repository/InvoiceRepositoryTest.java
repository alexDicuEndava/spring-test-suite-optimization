package com.example.test_presentation.bookkeeping.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import com.example.test_presentation.bookkeeping.model.Customer;
import com.example.test_presentation.bookkeeping.model.CustomerStatus;
import com.example.test_presentation.bookkeeping.model.Invoice;
import com.example.test_presentation.bookkeeping.model.InvoiceStatus;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
		"bookkeeping.demo.startup-delay-enabled=true",
		"bookkeeping.demo.startup-delay=2s",
		"spring.main.allow-bean-definition-overriding=true"
})
@Transactional
class InvoiceRepositoryTest {

	@Autowired
	CustomerRepository customerRepository;

	@Autowired
	InvoiceRepository invoiceRepository;

	@ParameterizedTest(name = "finds invoices by {0}")
	@CsvSource({
			"DRAFT,INV-REPO-1",
			"SENT,INV-REPO-2",
			"PAID,INV-REPO-3",
			"VOID,INV-REPO-4"
	})
	void findsInvoicesByStatus(InvoiceStatus status, String number) {
		Customer customer = customerRepository.save(new Customer(number, number.toLowerCase() + "@test.example",
				CustomerStatus.ACTIVE));
		invoiceRepository.save(new Invoice(number, customer, status, LocalDate.parse("2026-01-01"),
				LocalDate.parse("2026-01-31"), BigDecimal.ONE));

		assertThat(invoiceRepository.findByStatus(status))
				.extracting(Invoice::getInvoiceNumber)
				.contains(number);
	}

	@ParameterizedTest(name = "searches invoices {0}-{1}-{2}")
	@MethodSource("searchCases")
	void searchesInvoicesByCombinedFilters(String customerKey, String status, String overdueOn, List<String> expected) {
		SearchFixture fixture = searchFixture();
		Long customerId = switch (customerKey) {
			case "A" -> fixture.customerA().getId();
			case "B" -> fixture.customerB().getId();
			default -> null;
		};
		InvoiceStatus statusFilter = "NONE".equals(status) ? null : InvoiceStatus.valueOf(status);
		LocalDate overdueFilter = "NONE".equals(overdueOn) ? null : LocalDate.parse(overdueOn);

		List<Invoice> invoices = invoiceRepository.search(customerId, statusFilter, overdueFilter,
				List.of(InvoiceStatus.PAID, InvoiceStatus.VOID));

		assertThat(invoices).extracting(Invoice::getInvoiceNumber).containsExactlyElementsOf(expected);
	}

	static Stream<Arguments> searchCases() {
		return Stream.of(
				Arguments.of("A", "NONE", "NONE", numbers("INV-SEARCH-A1", "INV-SEARCH-A2", "INV-SEARCH-A3", "INV-SEARCH-A4")),
				Arguments.of("B", "NONE", "NONE", numbers("INV-SEARCH-B1", "INV-SEARCH-B2", "INV-SEARCH-B3")),
				Arguments.of("NONE", "SENT", "NONE", numbers("INV-SEARCH-A1", "INV-SEARCH-B1")),
				Arguments.of("NONE", "DRAFT", "NONE", numbers("INV-SEARCH-A2", "INV-SEARCH-B3")),
				Arguments.of("NONE", "PAID", "NONE", numbers("INV-SEARCH-A3", "INV-SEARCH-B2")),
				Arguments.of("NONE", "VOID", "NONE", numbers("INV-SEARCH-A4")),
				Arguments.of("NONE", "NONE", "2026-03-01", numbers("INV-SEARCH-A1", "INV-SEARCH-B1")),
				Arguments.of("A", "SENT", "2026-03-01", numbers("INV-SEARCH-A1")),
				Arguments.of("A", "DRAFT", "2026-03-01", numbers()),
				Arguments.of("A", "PAID", "2026-03-01", numbers()),
				Arguments.of("A", "VOID", "2026-03-01", numbers()),
				Arguments.of("B", "SENT", "2026-03-01", numbers("INV-SEARCH-B1")),
				Arguments.of("B", "DRAFT", "2026-06-01", numbers("INV-SEARCH-B3")),
				Arguments.of("B", "PAID", "2026-03-01", numbers()),
				Arguments.of("NONE", "NONE", "2026-02-10", numbers("INV-SEARCH-A1")),
				Arguments.of("NONE", "NONE", "2026-04-15", numbers("INV-SEARCH-A1", "INV-SEARCH-A2", "INV-SEARCH-B1")),
				Arguments.of("NONE", "NONE", "2026-06-15", numbers("INV-SEARCH-A1", "INV-SEARCH-A2", "INV-SEARCH-B1", "INV-SEARCH-B3")),
				Arguments.of("A", "NONE", "2026-06-15", numbers("INV-SEARCH-A1", "INV-SEARCH-A2")),
				Arguments.of("B", "NONE", "2026-06-15", numbers("INV-SEARCH-B1", "INV-SEARCH-B3")),
				Arguments.of("A", "NONE", "2026-02-01", numbers()),
				Arguments.of("A", "NONE", "2026-02-02", numbers("INV-SEARCH-A1")),
				Arguments.of("B", "NONE", "2026-02-16", numbers("INV-SEARCH-B1")),
				Arguments.of("B", "NONE", "2026-05-02", numbers("INV-SEARCH-B1", "INV-SEARCH-B3")),
				Arguments.of("NONE", "SENT", "2026-02-10", numbers("INV-SEARCH-A1")),
				Arguments.of("NONE", "SENT", "2026-03-01", numbers("INV-SEARCH-A1", "INV-SEARCH-B1")),
				Arguments.of("NONE", "DRAFT", "2026-06-01", numbers("INV-SEARCH-A2", "INV-SEARCH-B3")),
				Arguments.of("A", "SENT", "NONE", numbers("INV-SEARCH-A1")),
				Arguments.of("B", "DRAFT", "NONE", numbers("INV-SEARCH-B3")));
	}

	@ParameterizedTest(name = "finds invoices for customer {0}")
	@CsvSource({
			"A,INV-SEARCH-A1",
			"A,INV-SEARCH-A2",
			"A,INV-SEARCH-A3",
			"A,INV-SEARCH-A4",
			"B,INV-SEARCH-B1",
			"B,INV-SEARCH-B2",
			"B,INV-SEARCH-B3",
			"NONE,EMPTY"
	})
	void findsInvoicesByCustomer(String customerKey, String expectedNumber) {
		SearchFixture fixture = searchFixture();
		Long customerId = switch (customerKey) {
			case "A" -> fixture.customerA().getId();
			case "B" -> fixture.customerB().getId();
			default -> 999_999L;
		};

		List<Invoice> invoices = invoiceRepository.findByCustomerId(customerId);

		if ("EMPTY".equals(expectedNumber)) {
			assertThat(invoices).isEmpty();
		}
		else {
			assertThat(invoices).extracting(Invoice::getInvoiceNumber).contains(expectedNumber);
		}
	}

	private SearchFixture searchFixture() {
		Customer customerA = customerRepository.save(new Customer("Search A", "search-a@test.example", CustomerStatus.ACTIVE));
		Customer customerB = customerRepository.save(new Customer("Search B", "search-b@test.example", CustomerStatus.ACTIVE));
		invoiceRepository.save(new Invoice("INV-SEARCH-A1", customerA, InvoiceStatus.SENT, LocalDate.parse("2026-01-01"),
				LocalDate.parse("2026-02-01"), new BigDecimal("10.00")));
		invoiceRepository.save(new Invoice("INV-SEARCH-A2", customerA, InvoiceStatus.DRAFT, LocalDate.parse("2026-01-01"),
				LocalDate.parse("2026-04-01"), new BigDecimal("20.00")));
		invoiceRepository.save(new Invoice("INV-SEARCH-A3", customerA, InvoiceStatus.PAID, LocalDate.parse("2026-01-01"),
				LocalDate.parse("2026-01-15"), new BigDecimal("30.00")));
		invoiceRepository.save(new Invoice("INV-SEARCH-A4", customerA, InvoiceStatus.VOID, LocalDate.parse("2026-01-01"),
				LocalDate.parse("2026-01-20"), new BigDecimal("40.00")));
		invoiceRepository.save(new Invoice("INV-SEARCH-B1", customerB, InvoiceStatus.SENT, LocalDate.parse("2026-01-01"),
				LocalDate.parse("2026-02-15"), new BigDecimal("50.00")));
		invoiceRepository.save(new Invoice("INV-SEARCH-B2", customerB, InvoiceStatus.PAID, LocalDate.parse("2026-01-01"),
				LocalDate.parse("2026-02-15"), new BigDecimal("60.00")));
		invoiceRepository.save(new Invoice("INV-SEARCH-B3", customerB, InvoiceStatus.DRAFT, LocalDate.parse("2026-01-01"),
				LocalDate.parse("2026-05-01"), new BigDecimal("70.00")));
		return new SearchFixture(customerA, customerB);
	}

	private static List<String> numbers(String... numbers) {
		return Arrays.asList(numbers);
	}

	private record SearchFixture(Customer customerA, Customer customerB) {
	}
}
