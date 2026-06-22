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

import com.example.test_presentation.bookkeeping.dto.CustomerInvoiceSummaryResponse;
import com.example.test_presentation.bookkeeping.dto.CustomerRequest;
import com.example.test_presentation.bookkeeping.dto.CustomerResponse;
import com.example.test_presentation.bookkeeping.exception.ResourceNotFoundException;
import com.example.test_presentation.bookkeeping.model.CustomerStatus;
import com.example.test_presentation.bookkeeping.service.CustomerService;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(properties = {
		"bookkeeping.demo.startup-delay-enabled=true",
		"bookkeeping.demo.startup-delay=3s"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CustomerControllerTest {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	CustomerService customerService;

	@ParameterizedTest(name = "lists customers for {0}")
	@CsvSource({ "ACTIVE", "INACTIVE", "NONE" })
	void listsCustomersWithoutAuthentication(String status) throws Exception {
		Optional<CustomerStatus> filter = "NONE".equals(status) ? Optional.empty() : Optional.of(CustomerStatus.valueOf(status));
		given(customerService.findAll(filter))
				.willReturn(List.of(new CustomerResponse(1L, "Acme", "billing@acme.test", CustomerStatus.ACTIVE)));

		MockHttpServletRequestBuilder request = get("/api/customers");
		if (filter.isPresent()) {
			request.param("status", filter.get().name());
		}

		mockMvc.perform(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Acme"));
	}

	@ParameterizedTest(name = "returns customer {0}")
	@CsvSource({ "22,Globex,INACTIVE" })
	void returnsSingleCustomer(long id, String name, CustomerStatus status) throws Exception {
		given(customerService.find(eq(id))).willReturn(new CustomerResponse(id, name, "ap@globex.test", status));

		mockMvc.perform(get("/api/customers/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(status.name()));
	}

	@ParameterizedTest(name = "creates customer {0}")
	@CsvSource({
			"Northwind,ar@northwind.test,ACTIVE",
			"Initech,office@initech.test,INACTIVE"
	})
	void createsCustomerWhenAuthenticated(String name, String email, CustomerStatus status) throws Exception {
		given(customerService.create(any(CustomerRequest.class))).willReturn(new CustomerResponse(11L, name, email, status));

		mockMvc.perform(post("/api/customers")
						.with(httpBasic("demo", "demo"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"%s","email":"%s","status":"%s"}
								""".formatted(name, email, status)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value(name));
	}

	@ParameterizedTest(name = "rejects unauthenticated {0}")
	@MethodSource("writeRequests")
	void rejectsWritesWithoutAuthentication(MockHttpServletRequestBuilder request) throws Exception {
		mockMvc.perform(request)
				.andExpect(status().isUnauthorized());
	}

	static Stream<MockHttpServletRequestBuilder> writeRequests() {
		return Stream.of(
				post("/api/customers").contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Northwind\",\"email\":\"ar@northwind.test\",\"status\":\"ACTIVE\"}"),
				put("/api/customers/1").contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Northwind\",\"email\":\"ar@northwind.test\",\"status\":\"ACTIVE\"}"),
				delete("/api/customers/1"));
	}

	@ParameterizedTest(name = "rejects invalid customer {0}")
	@MethodSource("invalidCustomers")
	void validatesCustomerRequests(String body, String field) throws Exception {
		mockMvc.perform(post("/api/customers")
						.with(httpBasic("demo", "demo"))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors." + field).exists());
	}

	static Stream<Arguments> invalidCustomers() {
		return Stream.of(
				Arguments.of("{\"name\":\"\",\"email\":\"billing@acme.test\",\"status\":\"ACTIVE\"}", "name"),
				Arguments.of("{\"name\":\"Acme\",\"email\":\"\",\"status\":\"ACTIVE\"}", "email"),
				Arguments.of("{\"name\":\"Acme\",\"email\":\"not-an-email\",\"status\":\"ACTIVE\"}", "email"),
				Arguments.of("{\"name\":\"Acme\",\"email\":\"billing@acme.test\",\"status\":null}", "status"));
	}

	@ParameterizedTest(name = "deletes customer {0}")
	@CsvSource({ "44" })
	void deletesCustomerWhenAuthenticated(long id) throws Exception {
		mockMvc.perform(delete("/api/customers/{id}", id).with(httpBasic("demo", "demo")))
				.andExpect(status().isNoContent());
	}

	@ParameterizedTest(name = "returns invoice summary {0}")
	@CsvSource({
			"100,2026-03-01,3,150.00,120.00,80.00",
			"101,2026-03-01,2,75.50,75.50,0.00",
			"102,2026-04-01,4,400.00,300.00,300.00",
			"103,2026-01-15,1,20.00,20.00,0.00",
			"104,2026-05-10,5,999.99,500.00,250.00",
			"105,2026-02-28,0,0.00,0.00,0.00",
			"106,2026-06-01,6,1234.56,600.00,600.00",
			"107,2026-03-15,2,40.00,10.00,10.00",
			"108,2026-07-01,7,700.00,350.00,350.00",
			"109,2026-08-01,8,800.00,0.00,0.00",
			"110,2026-09-01,9,900.00,450.00,400.00",
			"111,2026-10-01,10,1000.00,1000.00,1000.00"
	})
	void returnsInvoiceSummaryWithoutAuthentication(long customerId, String overdueOn, long invoiceCount, String total,
			String open, String overdue) throws Exception {
		given(customerService.invoiceSummary(eq(customerId), eq(Optional.of(LocalDate.parse(overdueOn)))))
				.willReturn(new CustomerInvoiceSummaryResponse(customerId, invoiceCount, new BigDecimal(total),
						new BigDecimal(open), new BigDecimal(overdue)));

		mockMvc.perform(get("/api/customers/{id}/invoice-summary", customerId).param("overdueOn", overdueOn))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.invoiceCount").value(invoiceCount))
				.andExpect(jsonPath("$.totalInvoiced").value(Double.parseDouble(total)))
				.andExpect(jsonPath("$.openBalance").value(Double.parseDouble(open)))
				.andExpect(jsonPath("$.overdueBalance").value(Double.parseDouble(overdue)));
	}

	@ParameterizedTest(name = "missing summary customer {0}")
	@CsvSource({ "901", "902", "903", "904" })
	void invoiceSummaryMissingCustomerReturnsNotFound(long customerId) throws Exception {
		given(customerService.invoiceSummary(eq(customerId), eq(Optional.of(LocalDate.parse("2026-03-01")))))
				.willThrow(new ResourceNotFoundException("Customer %d was not found".formatted(customerId)));

		mockMvc.perform(get("/api/customers/{id}/invoice-summary", customerId).param("overdueOn", "2026-03-01"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Customer %d was not found".formatted(customerId)));
	}
}
