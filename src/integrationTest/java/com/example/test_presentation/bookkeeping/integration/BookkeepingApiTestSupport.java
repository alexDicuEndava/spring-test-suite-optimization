package com.example.test_presentation.bookkeeping.integration;

import com.example.test_presentation.bookkeeping.repository.CustomerRepository;
import com.example.test_presentation.bookkeeping.repository.InvoiceRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

abstract class BookkeepingApiTestSupport extends ApplicationIntegrationTest {

	static final Long acmeCustomerId = 100L;

	static final Long dormantCustomerId = 101L;

	static final Long sentInvoiceId = 200L;

	static final Long draftInvoiceId = 202L;

	@Autowired
	MockMvc mockMvc;

	@Autowired
	CustomerRepository customerRepository;

	@Autowired
	InvoiceRepository invoiceRepository;

	Long customerIdFor(String customerKey) {
		return switch (customerKey) {
			case "ACME" -> acmeCustomerId;
			case "DORMANT" -> dormantCustomerId;
			default -> throw new IllegalArgumentException("Unknown customer key: " + customerKey);
		};
	}
}
