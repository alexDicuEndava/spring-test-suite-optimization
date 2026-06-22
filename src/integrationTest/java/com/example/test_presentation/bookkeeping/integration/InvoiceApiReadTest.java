package com.example.test_presentation.bookkeeping.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.test_presentation.bookkeeping.model.InvoiceStatus;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class InvoiceApiReadTest extends BookkeepingApiTestSupport {

	@ParameterizedTest(name = "filters invoices {0}")
	@CsvSource({ "SENT,2,INV-SEED-001", "PAID,2,INV-SEED-002" })
	void filtersInvoicesThroughFullStack(String status, int size, String number) throws Exception {
		mockMvc.perform(get("/api/invoices").param("status", status))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(size)))
				.andExpect(jsonPath("$[0].invoiceNumber").value(number));
	}

	@ParameterizedTest(name = "returns invoice {0}")
	@CsvSource({ "INV-SEED-001,SENT,125.50" })
	void returnsSingleInvoiceThroughFullStack(String invoiceNumber, InvoiceStatus status, String total) throws Exception {
		mockMvc.perform(get("/api/invoices/{id}", sentInvoiceId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.invoiceNumber").value(invoiceNumber))
				.andExpect(jsonPath("$.customerId").value(acmeCustomerId))
				.andExpect(jsonPath("$.status").value(status.name()))
				.andExpect(jsonPath("$.total").value(Double.parseDouble(total)));
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
}
