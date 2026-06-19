package com.example.test_presentation.bookkeeping.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.test_presentation.bookkeeping.customer.Customer;
import com.example.test_presentation.bookkeeping.customer.CustomerRepository;
import com.example.test_presentation.bookkeeping.customer.CustomerStatus;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class InvoiceRepositorySliceTest {

	@Autowired
	InvoiceRepository invoiceRepository;

	@Autowired
	CustomerRepository customerRepository;

	@Test
	void findsInvoicesByCustomerWithTransactionalRollback() {
		Customer customer = customerRepository.save(new Customer("Acme", "billing@acme.test", CustomerStatus.ACTIVE));
		invoiceRepository.save(new Invoice("INV-1", customer, InvoiceStatus.PAID,
				LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"), new BigDecimal("15.00")));

		assertThat(invoiceRepository.findByCustomerId(customer.getId()))
				.extracting(Invoice::getInvoiceNumber)
				.containsExactly("INV-1");
	}
}
