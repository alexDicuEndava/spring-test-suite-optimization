package com.example.test_presentation.bookkeeping.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class InvoiceApiTest extends ApplicationIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	CustomerRepository customerRepository;

	@Autowired
	InvoiceRepository invoiceRepository;

	Long acmeCustomerId;

	Long dormantCustomerId;

	@BeforeEach
	void createTestData() {
		cleanTestData();

		Customer acme = customerRepository.save(new Customer("Acme Books", "billing@acme.test", CustomerStatus.ACTIVE));
		Customer dormant = customerRepository.save(new Customer("Dormant Ledger", "ap@dormant.test",
				CustomerStatus.INACTIVE));

		acmeCustomerId = acme.getId();
		dormantCustomerId = dormant.getId();

		invoiceRepository.saveAll(List.of(
				invoice("INV-SEED-001", acme, InvoiceStatus.SENT, "2026-02-10", "125.50"),
				invoice("INV-SEED-002", dormant, InvoiceStatus.PAID, "2026-02-11", "88.00"),
				invoice("INV-SEED-003", acme, InvoiceStatus.DRAFT, "2026-04-10", "50.00"),
				invoice("INV-SEED-004", acme, InvoiceStatus.PAID, "2026-01-15", "10.00"),
				invoice("INV-SEED-005", acme, InvoiceStatus.VOID, "2026-01-20", "15.00"),
				invoice("INV-SEED-006", dormant, InvoiceStatus.SENT, "2026-02-20", "44.50"),
				invoice("INV-SEED-007", dormant, InvoiceStatus.DRAFT, "2026-05-01", "20.00")));
	}

	@AfterEach
	void cleanTestData() {
		invoiceRepository.deleteAll();
		customerRepository.deleteAll();
	}

	@ParameterizedTest(name = "filters invoices {0}")
	@CsvSource({ "SENT,2,INV-SEED-001", "PAID,2,INV-SEED-002" })
	void filtersInvoicesThroughFullStack(String status, int size, String number) throws Exception {
		mockMvc.perform(get("/api/invoices").param("status", status))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(size)))
				.andExpect(jsonPath("$[0].invoiceNumber").value(number));
	}

	@ParameterizedTest(name = "filters invoices by combined query {0}-{1}-{2}")
	@CsvSource({
			"ACME,NONE,NONE,4,INV-SEED-001",
			"DORMANT,NONE,NONE,3,INV-SEED-002",
			"NONE,SENT,NONE,2,INV-SEED-001",
			"NONE,DRAFT,NONE,2,INV-SEED-003",
			"NONE,PAID,NONE,2,INV-SEED-002",
			"NONE,VOID,NONE,1,INV-SEED-005",
			"NONE,NONE,2026-03-01,2,INV-SEED-001",
			"ACME,SENT,2026-03-01,1,INV-SEED-001",
			"ACME,DRAFT,2026-03-01,0,NONE",
			"DORMANT,SENT,2026-03-01,1,INV-SEED-006",
			"DORMANT,DRAFT,2026-06-01,1,INV-SEED-007",
			"ACME,NONE,2026-06-01,2,INV-SEED-001",
			"DORMANT,NONE,2026-06-01,2,INV-SEED-006",
			"NONE,SENT,2026-02-15,1,INV-SEED-001",
			"NONE,SENT,2026-03-01,2,INV-SEED-001",
			"NONE,DRAFT,2026-05-15,2,INV-SEED-003",
			"ACME,PAID,2026-06-01,0,NONE",
			"DORMANT,PAID,NONE,1,INV-SEED-002",
			"DORMANT,NONE,2026-02-01,0,NONE",
			"ACME,NONE,2026-02-11,1,INV-SEED-001"
	})
	void filtersInvoicesByCombinedQuery(String customerKey, String status, String overdueOn, int size, String firstNumber)
			throws Exception {
		var request = get("/api/invoices");
		if (!"NONE".equals(customerKey)) {
			request.param("customerId", Long.toString(customerIdFor(customerKey)));
		}
		if (!"NONE".equals(status)) {
			request.param("status", status);
		}
		if (!"NONE".equals(overdueOn)) {
			request.param("overdueOn", overdueOn);
		}

		var result = mockMvc.perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(size)));
		if (size > 0) {
			result.andExpect(jsonPath("$[0].invoiceNumber").value(firstNumber));
		}
	}

	@ParameterizedTest(name = "rejects invalid invoice query {0}")
	@CsvSource({
			"customerId,abc",
			"status,MISSING",
			"overdueOn,not-a-date"
	})
	void rejectsInvalidInvoiceQueryParameters(String name, String value) throws Exception {
		mockMvc.perform(get("/api/invoices").param(name, value))
				.andExpect(status().isBadRequest());
	}

	@ParameterizedTest(name = "creates invoice {0}")
	@CsvSource({ "199.99" })
	void createsInvoiceWithStableGenerator(String total) throws Exception {
		mockMvc.perform(post("/api/invoices")
						.with(httpBasic("demo", "demo"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"customerId":%d,"status":"SENT","issueDate":"2026-03-01","dueDate":"2026-03-31","total":%s}
								""".formatted(acmeCustomerId, total)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.invoiceNumber").value("INV-TEST-001"));
	}

	private Long customerIdFor(String customerKey) {
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
