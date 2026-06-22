package com.example.test_presentation.bookkeeping.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.example.test_presentation.bookkeeping.dto.InvoiceRequest;
import com.example.test_presentation.bookkeeping.dto.InvoiceResponse;
import com.example.test_presentation.bookkeeping.model.InvoiceStatus;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class InvoiceControllerTest extends ControllerSliceTestSupport {

	@ParameterizedTest(name = "lists invoices for {0}")
	@CsvSource({ "DRAFT", "SENT", "PAID", "NONE" })
	void listsInvoicesWithoutAuthentication(String status) throws Exception {
		Optional<InvoiceStatus> filter = "NONE".equals(status) ? Optional.empty() : Optional.of(InvoiceStatus.valueOf(status));
		given(invoiceService.findAll(Optional.empty(), filter, Optional.empty()))
				.willReturn(List.of(response(1L, InvoiceStatus.SENT, "125.50")));

		MockHttpServletRequestBuilder request = get("/api/invoices");
		if (filter.isPresent()) {
			request.param("status", filter.get().name());
		}

		mockMvc.perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].invoiceNumber").value("INV-TEST"));
	}

	@ParameterizedTest(name = "returns invoice {0}")
	@CsvSource({ "33,PAID,120.00" })
	void returnsSingleInvoice(long id, InvoiceStatus status, String total) throws Exception {
		given(invoiceService.find(eq(id))).willReturn(response(id, status, total));

		mockMvc.perform(get("/api/invoices/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(status.name()));
	}

	@ParameterizedTest(name = "creates invoice {0}")
	@CsvSource({
			"SENT,125.50",
			"DRAFT,99.00"
	})
	void createsInvoiceWhenAuthenticated(InvoiceStatus status, String total) throws Exception {
		given(invoiceService.create(any(InvoiceRequest.class))).willReturn(response(12L, status, total));

		mockMvc.perform(post("/api/invoices")
						.with(httpBasic("demo", "demo"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"customerId":1,"status":"%s","issueDate":"2026-01-10","dueDate":"2026-02-10","total":%s}
								""".formatted(status, total)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value(status.name()));
	}

	@ParameterizedTest(name = "rejects unauthenticated invoice {0}")
	@MethodSource("writeRequests")
	void rejectsWritesWithoutAuthentication(MockHttpServletRequestBuilder request) throws Exception {
		mockMvc.perform(request)
				.andExpect(status().isUnauthorized());
	}

	static Stream<MockHttpServletRequestBuilder> writeRequests() {
		return Stream.of(
				post("/api/invoices").contentType(MediaType.APPLICATION_JSON)
						.content("{\"customerId\":1,\"status\":\"SENT\",\"issueDate\":\"2026-01-10\",\"dueDate\":\"2026-02-10\",\"total\":10}"),
				put("/api/invoices/1").contentType(MediaType.APPLICATION_JSON)
						.content("{\"customerId\":1,\"status\":\"SENT\",\"issueDate\":\"2026-01-10\",\"dueDate\":\"2026-02-10\",\"total\":10}"),
				delete("/api/invoices/1"));
	}

	@ParameterizedTest(name = "rejects invalid invoice {0}")
	@MethodSource("invalidInvoices")
	void validatesInvoiceRequests(String body, String field) throws Exception {
		mockMvc.perform(post("/api/invoices")
						.with(httpBasic("demo", "demo"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors." + field).exists());
	}

	static Stream<Arguments> invalidInvoices() {
		return Stream.of(
				Arguments.of("{\"customerId\":null,\"status\":\"SENT\",\"issueDate\":\"2026-01-10\",\"dueDate\":\"2026-02-10\",\"total\":10}", "customerId"),
				Arguments.of("{\"customerId\":1,\"status\":null,\"issueDate\":\"2026-01-10\",\"dueDate\":\"2026-02-10\",\"total\":10}", "status"),
				Arguments.of("{\"customerId\":1,\"status\":\"SENT\",\"issueDate\":null,\"dueDate\":\"2026-02-10\",\"total\":10}", "issueDate"),
				Arguments.of("{\"customerId\":1,\"status\":\"SENT\",\"issueDate\":\"2026-01-10\",\"dueDate\":null,\"total\":10}", "dueDate"),
				Arguments.of("{\"customerId\":1,\"status\":\"SENT\",\"issueDate\":\"2026-01-10\",\"dueDate\":\"2026-02-10\",\"total\":-1}", "total"));
	}

	@ParameterizedTest(name = "deletes invoice {0}")
	@CsvSource({ "45" })
	void deletesInvoiceWhenAuthenticated(long id) throws Exception {
		mockMvc.perform(delete("/api/invoices/{id}", id).with(httpBasic("demo", "demo")))
				.andExpect(status().isNoContent());
	}

	@ParameterizedTest(name = "lists invoices by filters {0}-{1}-{2}")
	@CsvSource({
			"1,SENT,2026-03-01,SENT,100.00",
			"1,DRAFT,2026-03-01,DRAFT,101.00",
			"1,NONE,2026-03-01,SENT,102.00",
			"2,SENT,2026-03-01,SENT,103.00",
			"2,PAID,NONE,PAID,104.00",
			"2,NONE,NONE,DRAFT,105.00",
			"3,VOID,NONE,VOID,106.00",
			"3,NONE,2026-04-01,DRAFT,107.00",
			"4,SENT,2026-05-01,SENT,108.00",
			"4,DRAFT,NONE,DRAFT,109.00",
			"5,PAID,2026-06-01,PAID,110.00",
			"5,NONE,NONE,SENT,111.00",
			"6,SENT,NONE,SENT,112.00",
			"6,NONE,2026-07-01,DRAFT,113.00",
			"7,DRAFT,2026-08-01,DRAFT,114.00",
			"7,NONE,NONE,PAID,115.00",
			"8,SENT,2026-09-01,SENT,116.00",
			"8,VOID,NONE,VOID,117.00",
			"9,NONE,2026-10-01,SENT,118.00",
			"9,DRAFT,NONE,DRAFT,119.00",
			"10,SENT,NONE,SENT,120.00",
			"10,NONE,2026-11-01,DRAFT,121.00",
			"11,PAID,NONE,PAID,122.00",
			"11,NONE,2026-12-01,SENT,123.00"
	})
	void listsInvoicesWithExtendedFilters(long customerId, String status, String overdueOn, InvoiceStatus responseStatus,
			String total) throws Exception {
		Optional<InvoiceStatus> statusFilter = "NONE".equals(status) ? Optional.empty() : Optional.of(InvoiceStatus.valueOf(status));
		Optional<LocalDate> overdueFilter = "NONE".equals(overdueOn) ? Optional.empty() : Optional.of(LocalDate.parse(overdueOn));
		given(invoiceService.findAll(eq(Optional.of(customerId)), eq(statusFilter), eq(overdueFilter)))
				.willReturn(List.of(response(20L + customerId, responseStatus, total)));

		MockHttpServletRequestBuilder request = get("/api/invoices").param("customerId", Long.toString(customerId));
		if (statusFilter.isPresent()) {
			request.param("status", statusFilter.get().name());
		}
		if (overdueFilter.isPresent()) {
			request.param("overdueOn", overdueOn);
		}

		mockMvc.perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].status").value(responseStatus.name()))
				.andExpect(jsonPath("$[0].total").value(Double.parseDouble(total)));
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

	private static InvoiceResponse response(long id, InvoiceStatus status, String total) {
		return new InvoiceResponse(id, "INV-TEST", 1L, status, LocalDate.parse("2026-01-10"),
				LocalDate.parse("2026-02-10"), new BigDecimal(total));
	}
}
