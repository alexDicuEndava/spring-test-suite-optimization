package com.example.test_presentation.bookkeeping.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.test_presentation.bookkeeping.model.Customer;
import com.example.test_presentation.bookkeeping.model.CustomerStatus;
import com.example.test_presentation.bookkeeping.model.Invoice;
import com.example.test_presentation.bookkeeping.model.InvoiceStatus;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
		"bookkeeping.demo.startup-delay-enabled=true",
		"bookkeeping.demo.startup-delay=500ms",
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
}
