package com.example.test_presentation.bookkeeping.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@Sql(scripts = {
		"/sql/bookkeeping-cleanup.sql",
		"/sql/reports-create.sql"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Sql(scripts = "/sql/bookkeeping-cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
class ReportApiIntegrationTest extends ApplicationIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void returnsAgingReportThroughReusableFullContext() throws Exception {
		mockMvc.perform(get("/api/reports/aging").param("asOf", "2026-06-22"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(3)))
				.andExpect(jsonPath("$[0].customerName").value("Acme Books"))
				.andExpect(jsonPath("$[0].invoiceCount").value(4))
				.andExpect(jsonPath("$[0].current").value(120.00))
				.andExpect(jsonPath("$[0].oneToThirty").value(80.00))
				.andExpect(jsonPath("$[0].thirtyOneToSixty").value(60.00))
				.andExpect(jsonPath("$[0].sixtyOnePlus").value(40.00))
				.andExpect(jsonPath("$[0].openBalance").value(300.00));
	}

	@Test
	void keepsReportRowsStableAndExcludesClosedInvoices() throws Exception {
		mockMvc.perform(get("/api/reports/aging").param("asOf", "2026-06-22"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].customerName").value("Acme Books"))
				.andExpect(jsonPath("$[1].customerName").value("Beacon Retail"))
				.andExpect(jsonPath("$[2].customerName").value("Cobalt Services"))
				.andExpect(jsonPath("$[*].openBalance", not(hasItem(999.00))))
				.andExpect(jsonPath("$[*].openBalance", not(hasItem(888.00))));
	}

	@Test
	void rejectsInvalidAgingDateThroughFullStack() throws Exception {
		mockMvc.perform(get("/api/reports/aging").param("asOf", "not-a-date"))
				.andExpect(status().isBadRequest());
	}
}
