package com.example.test_presentation.bookkeeping.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.example.test_presentation.bookkeeping.dto.InvoiceAgingReportResponse;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ReportControllerTest extends ControllerSliceTestSupport {

	@ParameterizedTest(name = "returns aging report {0}")
	@CsvSource({ "2026-06-22" })
	void returnsAgingReportWithoutAuthentication(String asOf) throws Exception {
		given(reportService.invoiceAging(LocalDate.parse(asOf))).willReturn(List.of(
				new InvoiceAgingReportResponse(300L, "Acme Books", 4, new BigDecimal("120.00"),
						new BigDecimal("80.00"), new BigDecimal("60.00"), new BigDecimal("40.00"),
						new BigDecimal("300.00"))));

		mockMvc.perform(get("/api/reports/aging").param("asOf", asOf))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].customerName").value("Acme Books"))
				.andExpect(jsonPath("$[0].invoiceCount").value(4))
				.andExpect(jsonPath("$[0].openBalance").value(300.00));

		verify(reportService).invoiceAging(LocalDate.parse(asOf));
	}

	@ParameterizedTest(name = "rejects invalid aging date {0}")
	@CsvSource({ "not-a-date", "2026-02-30", "today", "06/22/2026" })
	void rejectsInvalidAgingDate(String asOf) throws Exception {
		mockMvc.perform(get("/api/reports/aging").param("asOf", asOf))
				.andExpect(status().isBadRequest());
	}
}
