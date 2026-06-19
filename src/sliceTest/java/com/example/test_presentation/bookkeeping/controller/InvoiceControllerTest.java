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

import com.example.test_presentation.bookkeeping.config.SecurityConfig;
import com.example.test_presentation.bookkeeping.dto.InvoiceRequest;
import com.example.test_presentation.bookkeeping.dto.InvoiceResponse;
import com.example.test_presentation.bookkeeping.exception.RestExceptionHandler;
import com.example.test_presentation.bookkeeping.model.InvoiceStatus;
import com.example.test_presentation.bookkeeping.service.InvoiceService;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(InvoiceController.class)
@Import({ SecurityConfig.class, RestExceptionHandler.class })
@ImportAutoConfiguration({
		SecurityAutoConfiguration.class,
		SecurityFilterAutoConfiguration.class,
		ServletWebSecurityAutoConfiguration.class
})
class InvoiceControllerTest {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	InvoiceService invoiceService;

	@ParameterizedTest(name = "lists invoices for {0}")
	@CsvSource({ "DRAFT", "SENT", "PAID", "NONE" })
	void listsInvoicesWithoutAuthentication(String status) throws Exception {
		Optional<InvoiceStatus> filter = "NONE".equals(status) ? Optional.empty() : Optional.of(InvoiceStatus.valueOf(status));
		given(invoiceService.findAll(filter)).willReturn(List.of(response(1L, InvoiceStatus.SENT, "125.50")));

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

	private static InvoiceResponse response(long id, InvoiceStatus status, String total) {
		return new InvoiceResponse(id, "INV-TEST", 1L, status, LocalDate.parse("2026-01-10"),
				LocalDate.parse("2026-02-10"), new BigDecimal(total));
	}
}
