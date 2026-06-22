package com.example.test_presentation.bookkeeping.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CustomerApiReadTest extends BookkeepingApiTestSupport {

	@ParameterizedTest(name = "lists seeded customers {0}")
	@CsvSource({ "ACTIVE,Acme Books", "INACTIVE,Dormant Ledger" })
	void listsSeededCustomersThroughFullStack(String status, String name) throws Exception {
		mockMvc.perform(get("/api/customers").param("status", status))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].name").value(name));
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
}
