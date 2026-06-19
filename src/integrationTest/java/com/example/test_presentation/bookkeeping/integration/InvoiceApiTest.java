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
class InvoiceApiTest extends ApplicationIntegrationTest {

	@Autowired
	MockMvc mockMvc;

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
			"100,NONE,NONE,4,INV-SEED-001",
			"101,NONE,NONE,3,INV-SEED-002",
			"NONE,SENT,NONE,2,INV-SEED-001",
			"NONE,DRAFT,NONE,2,INV-SEED-003",
			"NONE,PAID,NONE,2,INV-SEED-002",
			"NONE,VOID,NONE,1,INV-SEED-005",
			"NONE,NONE,2026-03-01,2,INV-SEED-001",
			"100,SENT,2026-03-01,1,INV-SEED-001",
			"100,DRAFT,2026-03-01,0,NONE",
			"101,SENT,2026-03-01,1,INV-SEED-006",
			"101,DRAFT,2026-06-01,1,INV-SEED-007",
			"100,NONE,2026-06-01,2,INV-SEED-001",
			"101,NONE,2026-06-01,2,INV-SEED-006",
			"NONE,SENT,2026-02-15,1,INV-SEED-001",
			"NONE,SENT,2026-03-01,2,INV-SEED-001",
			"NONE,DRAFT,2026-05-15,2,INV-SEED-003",
			"100,PAID,2026-06-01,0,NONE",
			"101,PAID,NONE,1,INV-SEED-002",
			"101,NONE,2026-02-01,0,NONE",
			"100,NONE,2026-02-11,1,INV-SEED-001"
	})
	void filtersInvoicesByCombinedQuery(String customerId, String status, String overdueOn, int size, String firstNumber)
			throws Exception {
		var request = get("/api/invoices");
		if (!"NONE".equals(customerId)) {
			request.param("customerId", customerId);
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
								{"customerId":100,"status":"SENT","issueDate":"2026-03-01","dueDate":"2026-03-31","total":%s}
								""".formatted(total)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.invoiceNumber").value("INV-TEST-001"));
	}
}
