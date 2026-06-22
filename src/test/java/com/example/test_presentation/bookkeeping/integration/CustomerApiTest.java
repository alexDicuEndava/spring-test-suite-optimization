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

class CustomerApiTest extends ApplicationIntegrationTest {

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

	@ParameterizedTest(name = "lists seeded customers {0}")
	@CsvSource({ "ACTIVE,Acme Books", "INACTIVE,Dormant Ledger" })
	void listsSeededCustomersThroughFullStack(String status, String name) throws Exception {
		mockMvc.perform(get("/api/customers").param("status", status))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].name").value(name));
	}

	@ParameterizedTest(name = "creates customer {0}")
	@CsvSource({ "New Ledger,ledger@example.test" })
	void createsCustomerThroughFullStackWithSecurity(String name, String email) throws Exception {
		mockMvc.perform(post("/api/customers")
						.with(httpBasic("demo", "demo"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"%s","email":"%s","status":"ACTIVE"}
								""".formatted(name, email)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value(name));
	}

	@ParameterizedTest(name = "summarizes customer invoices {0}-{1}")
	@CsvSource({
			"ACME,2026-03-01,4,200.50,175.50,125.50",
			"ACME,2026-06-01,4,200.50,175.50,175.50",
			"ACME,2026-02-11,4,200.50,175.50,125.50",
			"ACME,2026-02-10,4,200.50,175.50,0.00",
			"ACME,2026-04-11,4,200.50,175.50,175.50",
			"DORMANT,2026-03-01,3,152.50,64.50,44.50",
			"DORMANT,2026-06-01,3,152.50,64.50,64.50",
			"DORMANT,2026-02-21,3,152.50,64.50,44.50",
			"DORMANT,2026-02-20,3,152.50,64.50,0.00",
			"DORMANT,2026-05-02,3,152.50,64.50,64.50",
			"ACME,2026-01-01,4,200.50,175.50,0.00",
			"DORMANT,2026-01-01,3,152.50,64.50,0.00",
			"ACME,2026-12-31,4,200.50,175.50,175.50"
	})
	void summarizesCustomerInvoicesThroughFullStack(String customerKey, String overdueOn, long invoiceCount, String total,
			String open, String overdue) throws Exception {
		long customerId = customerIdFor(customerKey);

		mockMvc.perform(get("/api/customers/{id}/invoice-summary", customerId).param("overdueOn", overdueOn))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.invoiceCount").value(invoiceCount))
				.andExpect(jsonPath("$.totalInvoiced").value(Double.parseDouble(total)))
				.andExpect(jsonPath("$.openBalance").value(Double.parseDouble(open)))
				.andExpect(jsonPath("$.overdueBalance").value(Double.parseDouble(overdue)));
	}

	@ParameterizedTest(name = "missing summary customer {0}")
	@CsvSource({ "900", "901", "902", "903" })
	void missingCustomerInvoiceSummaryReturnsNotFound(long customerId) throws Exception {
		mockMvc.perform(get("/api/customers/{id}/invoice-summary", customerId).param("overdueOn", "2026-03-01"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Customer %d was not found".formatted(customerId)));
	}

	@ParameterizedTest(name = "rejects invalid summary date {0}")
	@CsvSource({ "not-a-date" })
	void rejectsInvalidSummaryDate(String overdueOn) throws Exception {
		mockMvc.perform(get("/api/customers/100/invoice-summary").param("overdueOn", overdueOn))
				.andExpect(status().isBadRequest());
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
