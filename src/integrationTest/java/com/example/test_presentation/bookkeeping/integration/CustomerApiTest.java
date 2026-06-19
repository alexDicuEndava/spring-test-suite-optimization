package com.example.test_presentation.bookkeeping.integration;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@Sql(scripts = "/sql/bookkeeping-cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/customers-create.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/bookkeeping-cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class CustomerApiTest extends ApplicationIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@ParameterizedTest(name = "lists seeded customers {0}")
	@CsvSource({ "ACTIVE,Acme Books", "INACTIVE,Dormant Ledger" })
	void listsSeededCustomersThroughFullStack(String status, String name) throws Exception {
		mockMvc.perform(get("/api/customers").param("status", status))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].name").value(name));
	}

	@ParameterizedTest(name = "creates customer {0}")
	@CsvSource({ "New Ledger,ledger@example.test" })
	void createsCustomerThroughFullStackWithSecurity(String name, String email) throws Exception {
		mockMvc.perform(post("/api/customers")
						.with(httpBasic("demo", "demo"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"%s","email":"%s","status":"ACTIVE"}
								""".formatted(name, email)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value(name));
	}
}
