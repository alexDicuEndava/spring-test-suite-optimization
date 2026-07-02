package com.example.test_presentation.bookkeeping.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.test_presentation.bookkeeping.repository.InvoiceRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(scripts = {
		"/sql/bookkeeping-cleanup.sql",
		"/sql/reports-create.sql"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/bookkeeping-cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@SpringBootTest(properties = {
		"bookkeeping.demo.startup-delay-enabled=true",
		"bookkeeping.demo.startup-delay=3s"
})
@AutoConfigureMockMvc
@Import(BookkeepingTestConfiguration.class)
class ReportApiIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	InvoiceRepository invoiceRepository;

	@ParameterizedTest(name = "aging report {0} customer {2}")
	@CsvSource({
			"2026-03-02,0,Acme Books,4,260.00,40.00,0.00,0.00,300.00",
			"2026-03-02,1,Beacon Retail,3,300.00,0.00,0.00,0.00,300.00",
			"2026-03-02,2,Cobalt Services,3,66.00,0.00,0.00,0.00,66.00",
			"2026-04-23,0,Acme Books,4,260.00,0.00,40.00,0.00,300.00",
			"2026-04-23,1,Beacon Retail,3,300.00,0.00,0.00,0.00,300.00",
			"2026-04-23,2,Cobalt Services,3,33.00,33.00,0.00,0.00,66.00",
			"2026-05-01,0,Acme Books,4,260.00,0.00,0.00,40.00,300.00",
			"2026-05-01,1,Beacon Retail,3,230.00,70.00,0.00,0.00,300.00",
			"2026-05-01,2,Cobalt Services,3,33.00,33.00,0.00,0.00,66.00",
			"2026-06-01,0,Acme Books,4,200.00,0.00,60.00,40.00,300.00",
			"2026-06-01,1,Beacon Retail,3,230.00,0.00,70.00,0.00,300.00",
			"2026-06-01,2,Cobalt Services,3,11.00,22.00,33.00,0.00,66.00",
			"2026-06-21,0,Acme Books,4,120.00,80.00,60.00,40.00,300.00",
			"2026-06-21,1,Beacon Retail,3,230.00,0.00,70.00,0.00,300.00",
			"2026-06-21,2,Cobalt Services,3,0.00,33.00,0.00,33.00,66.00",
			"2026-06-22,0,Acme Books,4,120.00,80.00,60.00,40.00,300.00",
			"2026-06-22,1,Beacon Retail,3,200.00,30.00,70.00,0.00,300.00",
			"2026-06-22,2,Cobalt Services,3,0.00,11.00,22.00,33.00,66.00",
			"2026-07-22,0,Acme Books,4,0.00,120.00,80.00,100.00,300.00",
			"2026-07-22,1,Beacon Retail,3,0.00,200.00,30.00,70.00,300.00",
			"2026-07-22,2,Cobalt Services,3,0.00,0.00,11.00,55.00,66.00"
	})
	void returnsAgingReportThroughFullStack(String asOf, int row, String customerName, long invoiceCount,
			String current, String oneToThirty, String thirtyOneToSixty, String sixtyOnePlus, String openBalance)
			throws Exception {
		mockMvc.perform(get("/api/reports/aging").param("asOf", asOf))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(3)))
				.andExpect(jsonPath("$[%d].customerName".formatted(row)).value(customerName))
				.andExpect(jsonPath("$[%d].invoiceCount".formatted(row)).value(invoiceCount))
				.andExpect(jsonPath("$[%d].current".formatted(row)).value(Double.parseDouble(current)))
				.andExpect(jsonPath("$[%d].oneToThirty".formatted(row)).value(Double.parseDouble(oneToThirty)))
				.andExpect(jsonPath("$[%d].thirtyOneToSixty".formatted(row)).value(Double.parseDouble(thirtyOneToSixty)))
				.andExpect(jsonPath("$[%d].sixtyOnePlus".formatted(row)).value(Double.parseDouble(sixtyOnePlus)))
				.andExpect(jsonPath("$[%d].openBalance".formatted(row)).value(Double.parseDouble(openBalance)));
	}

	@Test
	void returnsRowsInCustomerNameOrder() throws Exception {
		mockMvc.perform(get("/api/reports/aging").param("asOf", "2026-06-22"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].customerName").value("Acme Books"))
				.andExpect(jsonPath("$[1].customerName").value("Beacon Retail"))
				.andExpect(jsonPath("$[2].customerName").value("Cobalt Services"));
	}

	@Test
	void excludesClosedInvoicesFromAgingBuckets() throws Exception {
		mockMvc.perform(get("/api/reports/aging").param("asOf", "2026-06-22"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].openBalance").value(300.00))
				.andExpect(jsonPath("$[*].openBalance", not(hasItem(999.00))))
				.andExpect(jsonPath("$[*].openBalance", not(hasItem(888.00))));
	}

	@Test
	void returnsEmptyReportWhenNoOpenInvoicesExist() throws Exception {
		invoiceRepository.deleteAll();

		mockMvc.perform(get("/api/reports/aging").param("asOf", "2026-06-22"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@ParameterizedTest(name = "rejects invalid aging date {0}")
	@CsvSource({ "not-a-date", "2026-02-30", "today", "06/22/2026" })
	void rejectsInvalidAgingDate(String asOf) throws Exception {
		mockMvc.perform(get("/api/reports/aging").param("asOf", asOf))
				.andExpect(status().isBadRequest());
	}
}
