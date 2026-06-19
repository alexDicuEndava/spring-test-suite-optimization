package com.example.test_presentation.bookkeeping.invoice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.example.test_presentation.bookkeeping.common.ResourceNotFoundException;
import com.example.test_presentation.bookkeeping.customer.Customer;
import com.example.test_presentation.bookkeeping.customer.CustomerRepository;
import com.example.test_presentation.bookkeeping.customer.CustomerStatus;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class InvoiceServiceUnitTest {

	private final InvoiceRepository invoiceRepository = Mockito.mock(InvoiceRepository.class);
	private final CustomerRepository customerRepository = Mockito.mock(CustomerRepository.class);
	private final InvoiceNumberGenerator invoiceNumberGenerator = Mockito.mock(InvoiceNumberGenerator.class);
	private final InvoiceService invoiceService = new InvoiceService(invoiceRepository, customerRepository,
			invoiceNumberGenerator);

	@Test
	void createsInvoiceWithGeneratedNumber() {
		Customer customer = new Customer("Acme", "billing@acme.test", CustomerStatus.ACTIVE);
		Invoice saved = new Invoice("INV-TEST-001", customer, InvoiceStatus.SENT,
				LocalDate.parse("2026-01-10"), LocalDate.parse("2026-02-10"), new BigDecimal("125.50"));
		given(customerRepository.findById(7L)).willReturn(Optional.of(customer));
		given(invoiceNumberGenerator.nextInvoiceNumber()).willReturn("INV-TEST-001");
		given(invoiceRepository.save(Mockito.any(Invoice.class))).willReturn(saved);

		InvoiceResponse response = invoiceService.create(new InvoiceRequest(7L, InvoiceStatus.SENT,
				LocalDate.parse("2026-01-10"), LocalDate.parse("2026-02-10"), new BigDecimal("125.50")));

		assertThat(response.invoiceNumber()).isEqualTo("INV-TEST-001");
		assertThat(response.total()).isEqualByComparingTo("125.50");
	}

	@Test
	void createFailsWhenCustomerIsMissing() {
		given(customerRepository.findById(404L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> invoiceService.create(new InvoiceRequest(404L, InvoiceStatus.SENT,
				LocalDate.parse("2026-01-10"), LocalDate.parse("2026-02-10"), BigDecimal.TEN)))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Customer 404");
	}

	@Test
	void listsInvoicesByStatus() {
		Customer customer = new Customer("Acme", "billing@acme.test", CustomerStatus.ACTIVE);
		given(invoiceRepository.findByStatus(InvoiceStatus.PAID))
				.willReturn(List.of(new Invoice("INV-PAID", customer, InvoiceStatus.PAID,
						LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"), BigDecimal.ONE)));

		List<InvoiceResponse> invoices = invoiceService.findAll(Optional.of(InvoiceStatus.PAID));

		assertThat(invoices).extracting(InvoiceResponse::status).containsExactly(InvoiceStatus.PAID);
	}

	@Test
	void updatesInvoiceTotals() {
		Customer customer = new Customer("Acme", "billing@acme.test", CustomerStatus.ACTIVE);
		Invoice invoice = new Invoice("INV-1", customer, InvoiceStatus.DRAFT,
				LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"), BigDecimal.ONE);
		given(invoiceRepository.findById(5L)).willReturn(Optional.of(invoice));

		InvoiceResponse response = invoiceService.update(5L, new InvoiceRequest(1L, InvoiceStatus.SENT,
				LocalDate.parse("2026-02-01"), LocalDate.parse("2026-02-28"), new BigDecimal("88.00")));

		assertThat(response.status()).isEqualTo(InvoiceStatus.SENT);
		assertThat(response.total()).isEqualByComparingTo("88.00");
	}
}
