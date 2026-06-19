package com.example.test_presentation.bookkeeping;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@Sql(scripts = "/sql/bookkeeping-cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/customers-create.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/invoices-create.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/bookkeeping-cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class InvoiceApiIntegrationTest extends OptimizedIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void filtersInvoicesThroughFullStack() throws Exception {
		mockMvc.perform(get("/api/invoices").param("status", "SENT"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].invoiceNumber").value("INV-SEED-001"));
	}

	@Test
	void createsInvoiceWithStableGenerator() throws Exception {
		mockMvc.perform(post("/api/invoices")
						.with(httpBasic("demo", "demo"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"customerId":100,"status":"SENT","issueDate":"2026-03-01","dueDate":"2026-03-31","total":199.99}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.invoiceNumber").value("INV-TEST-001"));
	}
}
