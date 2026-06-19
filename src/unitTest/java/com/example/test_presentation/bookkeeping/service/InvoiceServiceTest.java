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

import com.example.test_presentation.bookkeeping.dto.InvoiceRequest;
import com.example.test_presentation.bookkeeping.dto.InvoiceResponse;
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

class InvoiceServiceTest {

	private final InvoiceRepository invoiceRepository = Mockito.mock(InvoiceRepository.class);
	private final CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
	private final InvoiceNumberGenerator invoiceNumberGenerator = Mockito.mock(InvoiceNumberGenerator.class);
	private final InvoiceService invoiceService = new InvoiceService(invoiceRepository, customerRepository,
			invoiceNumberGenerator);

	@ParameterizedTest(name = "creates invoice {0}")
	@CsvSource({
			"1,INV-TEST-001,SENT,125.50",
			"2,INV-TEST-002,DRAFT,99.00",
			"3,INV-TEST-003,PAID,1000.10",
			"4,INV-TEST-004,VOID,10.00",
			"5,INV-TEST-005,SENT,42.42",
			"6,INV-TEST-006,DRAFT,300.00",
			"7,INV-TEST-007,PAID,77.77",
			"8,INV-TEST-008,SENT,5.25",
			"9,INV-TEST-009,DRAFT,880.00",
			"10,INV-TEST-010,PAID,12.34"
	})
	void createsInvoice(long customerId, String invoiceNumber, InvoiceStatus status, BigDecimal total) {
		Customer customer = customer("Acme");
		given(customerRepository.findById(customerId)).willReturn(Optional.of(customer));
		given(invoiceNumberGenerator.nextInvoiceNumber()).willReturn(invoiceNumber);
		given(invoiceRepository.save(Mockito.any(Invoice.class))).willAnswer((invocation) -> invocation.getArgument(0));

		InvoiceResponse response = invoiceService.create(request(customerId, status, total));

		assertThat(response.invoiceNumber()).isEqualTo(invoiceNumber);
		assertThat(response.status()).isEqualTo(status);
		assertThat(response.total()).isEqualByComparingTo(total);
	}

	@ParameterizedTest(name = "missing invoice customer {0}")
	@CsvSource({ "404", "405", "406" })
	void createMissingCustomerThrows(long customerId) {
		given(customerRepository.findById(customerId)).willReturn(Optional.empty());

		assertThatThrownBy(() -> invoiceService.create(request(customerId, InvoiceStatus.SENT, BigDecimal.TEN)))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Customer " + customerId);
	}

	@ParameterizedTest(name = "lists invoices for {0}")
	@MethodSource("listCases")
	void listsInvoices(Optional<InvoiceStatus> filter, List<Invoice> stored, List<InvoiceStatus> expectedStatuses) {
		given(invoiceRepository.search(Mockito.isNull(), Mockito.eq(filter.orElse(null)), Mockito.isNull(),
				Mockito.eq(List.of(InvoiceStatus.PAID, InvoiceStatus.VOID)))).willReturn(stored);

		List<InvoiceResponse> invoices = invoiceService.findAll(filter);

		assertThat(invoices).extracting(InvoiceResponse::status).containsExactlyElementsOf(expectedStatuses);
	}

	static Stream<Arguments> listCases() {
		return Stream.of(
				Arguments.of(Optional.empty(), List.of(invoice("INV-1", InvoiceStatus.DRAFT)), List.of(InvoiceStatus.DRAFT)),
				Arguments.of(Optional.of(InvoiceStatus.SENT), List.of(invoice("INV-2", InvoiceStatus.SENT)),
						List.of(InvoiceStatus.SENT)),
				Arguments.of(Optional.of(InvoiceStatus.PAID), List.of(invoice("INV-3", InvoiceStatus.PAID)),
						List.of(InvoiceStatus.PAID)),
				Arguments.of(Optional.of(InvoiceStatus.VOID), List.of(invoice("INV-4", InvoiceStatus.VOID)),
						List.of(InvoiceStatus.VOID)),
				Arguments.of(Optional.of(InvoiceStatus.SENT), List.of(), List.of()));
	}

	@ParameterizedTest(name = "updates invoice {0}")
	@CsvSource({
			"1,SENT,20.00",
			"2,PAID,21.00",
			"3,VOID,22.00",
			"4,DRAFT,23.00",
			"5,SENT,24.00",
			"6,PAID,25.00",
			"7,VOID,26.00",
			"8,DRAFT,27.00"
	})
	void updatesInvoice(long id, InvoiceStatus status, BigDecimal total) {
		Invoice invoice = invoice("INV-" + id, InvoiceStatus.DRAFT);
		given(invoiceRepository.findById(id)).willReturn(Optional.of(invoice));

		InvoiceResponse response = invoiceService.update(id, request(1L, status, total));

		assertThat(response.status()).isEqualTo(status);
		assertThat(response.total()).isEqualByComparingTo(total);
	}

	@ParameterizedTest(name = "missing invoice find {0}")
	@CsvSource({ "51", "52" })
	void findMissingInvoiceThrows(long id) {
		given(invoiceRepository.findById(id)).willReturn(Optional.empty());

		assertThatThrownBy(() -> invoiceService.find(id))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Invoice " + id);
	}

	@ParameterizedTest(name = "deletes existing invoice {0}")
	@CsvSource({ "71" })
	void deletesExistingInvoice(long id) {
		given(invoiceRepository.existsById(id)).willReturn(true);

		invoiceService.delete(id);

		verify(invoiceRepository).deleteById(id);
	}

	@ParameterizedTest(name = "missing invoice delete {0}")
	@CsvSource({ "81" })
	void deleteMissingInvoiceThrows(long id) {
		given(invoiceRepository.existsById(id)).willReturn(false);

		assertThatThrownBy(() -> invoiceService.delete(id))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Invoice " + id);
	}

	private static InvoiceRequest request(long customerId, InvoiceStatus status, BigDecimal total) {
		return new InvoiceRequest(customerId, status, LocalDate.parse("2026-01-10"), LocalDate.parse("2026-02-10"), total);
	}

	private static Customer customer(String name) {
		return new Customer(name, name.toLowerCase() + "@test.example", CustomerStatus.ACTIVE);
	}

	private static Invoice invoice(String number, InvoiceStatus status) {
		return new Invoice(number, customer("Acme"), status, LocalDate.parse("2026-01-01"),
				LocalDate.parse("2026-01-31"), BigDecimal.ONE);
	}
}
