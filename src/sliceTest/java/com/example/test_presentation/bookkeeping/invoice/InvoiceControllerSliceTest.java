package com.example.test_presentation.bookkeeping.invoice;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.example.test_presentation.bookkeeping.config.SecurityConfig;

import org.junit.jupiter.api.Test;
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

@WebMvcTest(InvoiceController.class)
@Import(SecurityConfig.class)
@ImportAutoConfiguration({
		SecurityAutoConfiguration.class,
		SecurityFilterAutoConfiguration.class,
		ServletWebSecurityAutoConfiguration.class
})
class InvoiceControllerSliceTest {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	InvoiceService invoiceService;

	@Test
	void listsInvoicesWithoutAuthentication() throws Exception {
		given(invoiceService.findAll(Optional.of(InvoiceStatus.SENT)))
				.willReturn(List.of(new InvoiceResponse(1L, "INV-1", 9L, InvoiceStatus.SENT,
						LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"), BigDecimal.TEN)));

		mockMvc.perform(get("/api/invoices").param("status", "SENT"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].invoiceNumber").value("INV-1"));
	}

	@Test
	void createsInvoiceWhenAuthenticated() throws Exception {
		given(invoiceService.create(any(InvoiceRequest.class)))
				.willReturn(new InvoiceResponse(3L, "INV-TEST", 2L, InvoiceStatus.SENT,
						LocalDate.parse("2026-01-10"), LocalDate.parse("2026-02-10"), new BigDecimal("44.00")));

		mockMvc.perform(post("/api/invoices")
						.with(httpBasic("demo", "demo"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"customerId":2,"status":"SENT","issueDate":"2026-01-10","dueDate":"2026-02-10","total":44.00}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.invoiceNumber").value("INV-TEST"));
	}

	@Test
	void rejectsUnauthenticatedInvoiceWrites() throws Exception {
		mockMvc.perform(post("/api/invoices")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"customerId":2,"status":"SENT","issueDate":"2026-01-10","dueDate":"2026-02-10","total":44.00}
								"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void validatesInvoiceRequests() throws Exception {
		mockMvc.perform(post("/api/invoices")
						.with(httpBasic("demo", "demo"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"status":"SENT","issueDate":"2026-01-10","dueDate":"2026-02-10","total":44.00}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.customerId").exists());
	}
}
