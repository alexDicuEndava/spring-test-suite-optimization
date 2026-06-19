package com.example.test_presentation.bookkeeping.customer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import com.example.test_presentation.bookkeeping.config.SecurityConfig;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CustomerController.class)
@Import(SecurityConfig.class)
@ImportAutoConfiguration({
		SecurityAutoConfiguration.class,
		SecurityFilterAutoConfiguration.class,
		ServletWebSecurityAutoConfiguration.class
})
class CustomerControllerSliceTest {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	CustomerService customerService;

	@Test
	void listsCustomersWithoutAuthentication() throws Exception {
		given(customerService.findAll(Optional.of(CustomerStatus.ACTIVE)))
				.willReturn(List.of(new CustomerResponse(1L, "Acme", "billing@acme.test", CustomerStatus.ACTIVE)));

		mockMvc.perform(get("/api/customers").param("status", "ACTIVE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Acme"));
	}

	@Test
	void createsCustomerWhenAuthenticated() throws Exception {
		given(customerService.create(any(CustomerRequest.class)))
				.willReturn(new CustomerResponse(11L, "Northwind", "ar@northwind.test", CustomerStatus.ACTIVE));

		mockMvc.perform(post("/api/customers")
						.with(httpBasic("demo", "demo"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Northwind","email":"ar@northwind.test","status":"ACTIVE"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(11));
	}

	@Test
	void rejectsWritesWithoutAuthentication() throws Exception {
		mockMvc.perform(post("/api/customers")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Northwind","email":"ar@northwind.test","status":"ACTIVE"}
								"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void validatesCustomerRequests() throws Exception {
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

	@Test
	void returnsSingleCustomer() throws Exception {
		given(customerService.find(eq(22L)))
				.willReturn(new CustomerResponse(22L, "Globex", "ap@globex.test", CustomerStatus.INACTIVE));

		mockMvc.perform(get("/api/customers/22"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("INACTIVE"));
	}
}
