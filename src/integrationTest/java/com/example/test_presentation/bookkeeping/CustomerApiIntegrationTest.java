package com.example.test_presentation.bookkeeping;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@Sql(scripts = "/sql/bookkeeping-cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/customers-create.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/bookkeeping-cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class CustomerApiIntegrationTest extends OptimizedIntegrationTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void listsSeededCustomersThroughFullStack() throws Exception {
		mockMvc.perform(get("/api/customers").param("status", "ACTIVE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].name").value("Acme Books"));
	}

	@Test
	void createsCustomerThroughFullStackWithSecurity() throws Exception {
		mockMvc.perform(post("/api/customers")
						.with(httpBasic("demo", "demo"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"New Ledger","email":"ledger@example.test","status":"ACTIVE"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("New Ledger"));
	}
}
