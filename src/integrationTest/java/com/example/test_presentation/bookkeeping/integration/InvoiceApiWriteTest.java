package com.example.test_presentation.bookkeeping.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.test_presentation.bookkeeping.model.InvoiceStatus;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;

class InvoiceApiWriteTest extends MutableBookkeepingApiTestSupport {

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

	@ParameterizedTest(name = "updates invoice {0}")
	@CsvSource({ "PAID,2026-03-10,2026-03-20,225.75" })
	void updatesInvoiceThroughFullStackWithSecurity(InvoiceStatus status, String issueDate, String dueDate, String total)
			throws Exception {
		mockMvc.perform(put("/api/invoices/{id}", sentInvoiceId)
						.with(httpBasic("demo", "demo"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"customerId":%d,"status":"%s","issueDate":"%s","dueDate":"%s","total":%s}
								""".formatted(acmeCustomerId, status, issueDate, dueDate, total)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(sentInvoiceId))
				.andExpect(jsonPath("$.invoiceNumber").value("INV-SEED-001"))
				.andExpect(jsonPath("$.status").value(status.name()))
				.andExpect(jsonPath("$.issueDate").value(issueDate))
				.andExpect(jsonPath("$.dueDate").value(dueDate))
				.andExpect(jsonPath("$.total").value(Double.parseDouble(total)));

		mockMvc.perform(get("/api/invoices/{id}", sentInvoiceId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(status.name()))
				.andExpect(jsonPath("$.total").value(Double.parseDouble(total)));
	}

	@ParameterizedTest(name = "deletes invoice {0}")
	@CsvSource({ "INV-SEED-003" })
	void deletesInvoiceThroughFullStackWithSecurity(String invoiceNumber) throws Exception {
		mockMvc.perform(delete("/api/invoices/{id}", draftInvoiceId).with(httpBasic("demo", "demo")))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/invoices/{id}", draftInvoiceId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Invoice %d was not found".formatted(draftInvoiceId)));

		mockMvc.perform(get("/api/invoices").param("customerId", Long.toString(acmeCustomerId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.invoiceNumber == '%s')]".formatted(invoiceNumber)).doesNotExist());
	}

	@ParameterizedTest(name = "rejects invoice write without auth {0}")
	@CsvSource({ "POST", "PUT", "DELETE" })
	void rejectsInvoiceWritesWithoutAuthentication(String method) throws Exception {
		var request = switch (method) {
			case "POST" -> post("/api/invoices")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"customerId":%d,"status":"SENT","issueDate":"2026-03-01","dueDate":"2026-03-31","total":199.99}
							""".formatted(acmeCustomerId));
			case "PUT" -> put("/api/invoices/{id}", sentInvoiceId)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"customerId":%d,"status":"SENT","issueDate":"2026-03-01","dueDate":"2026-03-31","total":199.99}
							""".formatted(acmeCustomerId));
			case "DELETE" -> delete("/api/invoices/{id}", sentInvoiceId);
			default -> throw new IllegalArgumentException("Unsupported method: " + method);
		};

		mockMvc.perform(request)
				.andExpect(status().isUnauthorized());
	}

	@ParameterizedTest(name = "rejects invalid invoice payload {0}")
	@CsvSource(value = {
			"customerId|{\"customerId\":null,\"status\":\"SENT\",\"issueDate\":\"2026-03-01\",\"dueDate\":\"2026-03-31\",\"total\":199.99}",
			"status|{\"customerId\":1,\"status\":null,\"issueDate\":\"2026-03-01\",\"dueDate\":\"2026-03-31\",\"total\":199.99}",
			"issueDate|{\"customerId\":1,\"status\":\"SENT\",\"issueDate\":null,\"dueDate\":\"2026-03-31\",\"total\":199.99}",
			"dueDate|{\"customerId\":1,\"status\":\"SENT\",\"issueDate\":\"2026-03-01\",\"dueDate\":null,\"total\":199.99}",
			"total|{\"customerId\":1,\"status\":\"SENT\",\"issueDate\":\"2026-03-01\",\"dueDate\":\"2026-03-31\",\"total\":-1.00}"
	}, delimiter = '|')
	void rejectsInvalidInvoicePayloadsThroughFullStack(String field, String body) throws Exception {
		mockMvc.perform(post("/api/invoices")
						.with(httpBasic("demo", "demo"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors." + field).exists());
	}

	@ParameterizedTest(name = "missing invoice customer {0}")
	@CsvSource({ "999999" })
	void rejectsInvoiceCreateForMissingCustomer(long customerId) throws Exception {
		mockMvc.perform(post("/api/invoices")
						.with(httpBasic("demo", "demo"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"customerId":%d,"status":"SENT","issueDate":"2026-03-01","dueDate":"2026-03-31","total":199.99}
								""".formatted(customerId)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Customer %d was not found".formatted(customerId)));
	}
}
