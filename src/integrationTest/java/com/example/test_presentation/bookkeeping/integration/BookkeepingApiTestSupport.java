package com.example.test_presentation.bookkeeping.integration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.example.test_presentation.bookkeeping.model.Customer;
import com.example.test_presentation.bookkeeping.model.CustomerStatus;
import com.example.test_presentation.bookkeeping.model.Invoice;
import com.example.test_presentation.bookkeeping.model.InvoiceStatus;
import com.example.test_presentation.bookkeeping.repository.CustomerRepository;
import com.example.test_presentation.bookkeeping.repository.InvoiceRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

abstract class BookkeepingApiTestSupport extends ApplicationIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	CustomerRepository customerRepository;

	@Autowired
	InvoiceRepository invoiceRepository;

	Long acmeCustomerId;

	Long dormantCustomerId;

	Long sentInvoiceId;

	Long draftInvoiceId;

	@BeforeEach
	void createTestData() {
		cleanTestData();

		Customer acme = customerRepository.save(new Customer("Acme Books", "billing@acme.test", CustomerStatus.ACTIVE));
		Customer dormant = customerRepository.save(new Customer("Dormant Ledger", "ap@dormant.test",
				CustomerStatus.INACTIVE));

		acmeCustomerId = acme.getId();
		dormantCustomerId = dormant.getId();

		List<Invoice> invoices = invoiceRepository.saveAll(List.of(
				invoice("INV-SEED-001", acme, InvoiceStatus.SENT, "2026-02-10", "125.50"),
				invoice("INV-SEED-002", dormant, InvoiceStatus.PAID, "2026-02-11", "88.00"),
				invoice("INV-SEED-003", acme, InvoiceStatus.DRAFT, "2026-04-10", "50.00"),
				invoice("INV-SEED-004", acme, InvoiceStatus.PAID, "2026-01-15", "10.00"),
				invoice("INV-SEED-005", acme, InvoiceStatus.VOID, "2026-01-20", "15.00"),
				invoice("INV-SEED-006", dormant, InvoiceStatus.SENT, "2026-02-20", "44.50"),
				invoice("INV-SEED-007", dormant, InvoiceStatus.DRAFT, "2026-05-01", "20.00")));
		sentInvoiceId = invoices.getFirst().getId();
		draftInvoiceId = invoices.get(2).getId();
	}

	@AfterEach
	void cleanTestData() {
		invoiceRepository.deleteAll();
		customerRepository.deleteAll();
	}

	Long customerIdFor(String customerKey) {
		return switch (customerKey) {
			case "ACME" -> acmeCustomerId;
			case "DORMANT" -> dormantCustomerId;
			default -> throw new IllegalArgumentException("Unknown customer key: " + customerKey);
		};
	}

	private static Invoice invoice(String invoiceNumber, Customer customer, InvoiceStatus status, String dueDate,
			String total) {
		return new Invoice(invoiceNumber, customer, status, LocalDate.parse("2026-01-10"), LocalDate.parse(dueDate),
				new BigDecimal(total));
	}
}
