package com.example.test_presentation.bookkeeping;

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
import com.example.test_presentation.bookkeeping.invoice.Invoice;
import com.example.test_presentation.bookkeeping.invoice.InvoiceNumberGenerator;
import com.example.test_presentation.bookkeeping.invoice.InvoiceRepository;
import com.example.test_presentation.bookkeeping.invoice.InvoiceRequest;
import com.example.test_presentation.bookkeeping.invoice.InvoiceResponse;
import com.example.test_presentation.bookkeeping.invoice.InvoiceService;
import com.example.test_presentation.bookkeeping.invoice.InvoiceStatus;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
		BadSuiteProperties.STARTUP_DELAY_ENABLED,
		BadSuiteProperties.STARTUP_DELAY,
		"bookkeeping.bad-suite.case=invoice-service"
})
class InvoiceServiceBadSuiteTest {

	@Autowired
	InvoiceService invoiceService;

	@MockitoBean
	InvoiceRepository invoiceRepository;

	@MockitoBean
	CustomerRepository customerRepository;

	@MockitoBean
	InvoiceNumberGenerator invoiceNumberGenerator;

	@Test
	void createsInvoiceWithPerClassMockBean() {
		Customer customer = new Customer("Acme", "billing@acme.test", CustomerStatus.ACTIVE);
		Invoice saved = new Invoice("INV-MOCK-001", customer, InvoiceStatus.SENT,
				LocalDate.parse("2026-01-10"), LocalDate.parse("2026-02-10"), new BigDecimal("125.50"));
		given(customerRepository.findById(7L)).willReturn(Optional.of(customer));
		given(invoiceNumberGenerator.nextInvoiceNumber()).willReturn("INV-MOCK-001");
		given(invoiceRepository.save(Mockito.any(Invoice.class))).willReturn(saved);

		InvoiceResponse response = invoiceService.create(new InvoiceRequest(7L, InvoiceStatus.SENT,
				LocalDate.parse("2026-01-10"), LocalDate.parse("2026-02-10"), new BigDecimal("125.50")));

		assertThat(response.invoiceNumber()).isEqualTo("INV-MOCK-001");
		assertThat(response.total()).isEqualByComparingTo("125.50");
	}

	@Test
	void createFailsWhenCustomerIsMissing() {
		given(customerRepository.findById(404L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> invoiceService.create(new InvoiceRequest(404L, InvoiceStatus.SENT,
				LocalDate.parse("2026-01-10"), LocalDate.parse("2026-02-10"), BigDecimal.ONE)))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Customer 404");
	}

	@Test
	void filtersInvoicesThroughFullSpringContextAndMockBeans() {
		Customer customer = new Customer("Acme", "billing@acme.test", CustomerStatus.ACTIVE);
		given(invoiceRepository.findByStatus(InvoiceStatus.PAID))
				.willReturn(List.of(new Invoice("INV-PAID", customer, InvoiceStatus.PAID,
						LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"), BigDecimal.ONE)));

		assertThat(invoiceService.findAll(Optional.of(InvoiceStatus.PAID)))
				.extracting(InvoiceResponse::status)
				.containsExactly(InvoiceStatus.PAID);
	}

	@Test
	void updatesInvoiceThroughFullSpringContextAndMockBeans() {
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
