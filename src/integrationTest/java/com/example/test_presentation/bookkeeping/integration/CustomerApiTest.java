package com.example.test_presentation.bookkeeping.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@Sql(scripts = "/sql/bookkeeping-cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/customers-create.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/invoices-create.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/bookkeeping-cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class CustomerApiTest extends ApplicationIntegrationTest {

	@Autowired
	MockMvc mockMvc;

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
			"100,2026-03-01,4,200.50,175.50,125.50",
			"100,2026-06-01,4,200.50,175.50,175.50",
			"100,2026-02-11,4,200.50,175.50,125.50",
			"100,2026-02-10,4,200.50,175.50,0.00",
			"100,2026-04-11,4,200.50,175.50,175.50",
			"101,2026-03-01,3,152.50,64.50,44.50",
			"101,2026-06-01,3,152.50,64.50,64.50",
			"101,2026-02-21,3,152.50,64.50,44.50",
			"101,2026-02-20,3,152.50,64.50,0.00",
			"101,2026-05-02,3,152.50,64.50,64.50",
			"100,2026-01-01,4,200.50,175.50,0.00",
			"101,2026-01-01,3,152.50,64.50,0.00",
			"100,2026-12-31,4,200.50,175.50,175.50"
	})
	void summarizesCustomerInvoicesThroughFullStack(long customerId, String overdueOn, long invoiceCount, String total,
			String open, String overdue) throws Exception {
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
}
