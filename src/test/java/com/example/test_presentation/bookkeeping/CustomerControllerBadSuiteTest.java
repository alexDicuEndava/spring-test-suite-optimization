package com.example.test_presentation.bookkeeping;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
		BadSuiteProperties.STARTUP_DELAY_ENABLED,
		BadSuiteProperties.STARTUP_DELAY,
		"bookkeeping.bad-suite.case=customer-controller"
})
@AutoConfigureMockMvc
@Sql(scripts = "/sql/bookkeeping-cleanup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/customers-create.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/bookkeeping-cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class CustomerControllerBadSuiteTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void listsCustomersThroughFullSpringContext() throws Exception {
		mockMvc.perform(get("/api/customers").param("status", "ACTIVE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].name").value("Acme Books"));
	}

	@Test
	@WithMockUser
	void createsCustomerThroughFullSpringContext() throws Exception {
		mockMvc.perform(post("/api/customers")
						.with(httpBasic("demo", "demo"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Northwind","email":"ar@northwind.test","status":"ACTIVE"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Northwind"));
	}

	@Test
	void rejectsUnauthenticatedWritesThroughFullSpringContext() throws Exception {
		mockMvc.perform(post("/api/customers")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Northwind","email":"ar@northwind.test","status":"ACTIVE"}
								"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser
	void validatesCustomersThroughFullSpringContext() throws Exception {
		mockMvc.perform(post("/api/customers")
						.with(httpBasic("demo", "demo"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"","email":"not-an-email","status":"ACTIVE"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.name").exists())
				.andExpect(jsonPath("$.fieldErrors.email").exists());
	}
}
