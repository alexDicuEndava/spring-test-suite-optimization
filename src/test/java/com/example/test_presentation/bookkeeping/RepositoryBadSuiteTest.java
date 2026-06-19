package com.example.test_presentation.bookkeeping;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.test_presentation.bookkeeping.customer.Customer;
import com.example.test_presentation.bookkeeping.customer.CustomerRepository;
import com.example.test_presentation.bookkeeping.customer.CustomerStatus;
import com.example.test_presentation.bookkeeping.invoice.Invoice;
import com.example.test_presentation.bookkeeping.invoice.InvoiceRepository;
import com.example.test_presentation.bookkeeping.invoice.InvoiceStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest(properties = {
		BadSuiteProperties.STARTUP_DELAY_ENABLED,
		BadSuiteProperties.STARTUP_DELAY,
		"bookkeeping.bad-suite.case=repository"
})
@Sql(scripts = "/sql/bookkeeping-cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/bookkeeping-cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class RepositoryBadSuiteTest {

	@Autowired
	CustomerRepository customerRepository;

	@Autowired
	InvoiceRepository invoiceRepository;

	@Test
	void findsCustomersByStatusUsingFullContextInsteadOfJpaSlice() {
		customerRepository.save(new Customer("Acme", "billing@acme.test", CustomerStatus.ACTIVE));
		customerRepository.save(new Customer("Dormant", "ap@dormant.test", CustomerStatus.INACTIVE));

		assertThat(customerRepository.findByStatus(CustomerStatus.ACTIVE))
				.extracting(Customer::getName)
				.containsExactly("Acme");
	}

	@Test
	void findsInvoicesByCustomerUsingFullContextInsteadOfJpaSlice() {
		Customer customer = customerRepository.save(new Customer("Acme", "billing@acme.test", CustomerStatus.ACTIVE));
		invoiceRepository.save(new Invoice("INV-1", customer, InvoiceStatus.PAID,
				LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"), new BigDecimal("15.00")));

		assertThat(invoiceRepository.findByCustomerId(customer.getId()))
				.extracting(Invoice::getInvoiceNumber)
				.containsExactly("INV-1");
	}
}
