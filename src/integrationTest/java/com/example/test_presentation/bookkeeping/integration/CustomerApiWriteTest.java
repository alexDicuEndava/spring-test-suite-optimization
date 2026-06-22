package com.example.test_presentation.bookkeeping.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.test_presentation.bookkeeping.model.Customer;
import com.example.test_presentation.bookkeeping.model.CustomerStatus;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;

class CustomerApiWriteTest extends BookkeepingApiTestSupport {

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

	@ParameterizedTest(name = "updates customer {0}")
	@CsvSource({ "Acme Books International,billing.intl@acme.test,INACTIVE" })
	void updatesCustomerThroughFullStackWithSecurity(String name, String email, CustomerStatus status) throws Exception {
		mockMvc.perform(put("/api/customers/{id}", acmeCustomerId)
						.with(httpBasic("demo", "demo"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"%s","email":"%s","status":"%s"}
								""".formatted(name, email, status)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(acmeCustomerId))
				.andExpect(jsonPath("$.name").value(name))
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.status").value(status.name()));

		mockMvc.perform(get("/api/customers/{id}", acmeCustomerId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value(name))
				.andExpect(jsonPath("$.status").value(status.name()));
	}

	@ParameterizedTest(name = "deletes customer {0}")
	@CsvSource({ "Temporary Ledger,temp@example.test" })
	void deletesCustomerThroughFullStackWithSecurity(String name, String email) throws Exception {
		Customer customer = customerRepository.save(new Customer(name, email, CustomerStatus.ACTIVE));

		mockMvc.perform(delete("/api/customers/{id}", customer.getId()).with(httpBasic("demo", "demo")))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/customers/{id}", customer.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Customer %d was not found".formatted(customer.getId())));
	}

	@ParameterizedTest(name = "rejects customer write without auth {0}")
	@CsvSource({ "POST", "PUT", "DELETE" })
	void rejectsCustomerWritesWithoutAuthentication(String method) throws Exception {
		var request = switch (method) {
			case "POST" -> post("/api/customers")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\":\"New Ledger\",\"email\":\"ledger@example.test\",\"status\":\"ACTIVE\"}");
			case "PUT" -> put("/api/customers/{id}", acmeCustomerId)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"name\":\"Acme Books\",\"email\":\"billing@acme.test\",\"status\":\"ACTIVE\"}");
			case "DELETE" -> delete("/api/customers/{id}", acmeCustomerId);
			default -> throw new IllegalArgumentException("Unsupported method: " + method);
		};

		mockMvc.perform(request)
				.andExpect(status().isUnauthorized());
	}

	@ParameterizedTest(name = "rejects invalid customer payload {0}")
	@CsvSource(value = {
			"name|{\"name\":\"\",\"email\":\"billing@acme.test\",\"status\":\"ACTIVE\"}",
			"email|{\"name\":\"Acme Books\",\"email\":\"not-an-email\",\"status\":\"ACTIVE\"}",
			"status|{\"name\":\"Acme Books\",\"email\":\"billing@acme.test\",\"status\":null}"
	}, delimiter = '|')
	void rejectsInvalidCustomerPayloadsThroughFullStack(String field, String body) throws Exception {
		mockMvc.perform(post("/api/customers")
						.with(httpBasic("demo", "demo"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors." + field).exists());
	}
}
