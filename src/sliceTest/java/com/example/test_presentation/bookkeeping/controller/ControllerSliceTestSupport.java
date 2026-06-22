package com.example.test_presentation.bookkeeping.controller;

import com.example.test_presentation.bookkeeping.config.SecurityConfig;
import com.example.test_presentation.bookkeeping.exception.RestExceptionHandler;
import com.example.test_presentation.bookkeeping.service.CustomerService;
import com.example.test_presentation.bookkeeping.service.InvoiceService;
import com.example.test_presentation.bookkeeping.service.ReportService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({ CustomerController.class, InvoiceController.class, ReportController.class })
@Import({ SecurityConfig.class, RestExceptionHandler.class })
@ImportAutoConfiguration({
		SecurityAutoConfiguration.class,
		SecurityFilterAutoConfiguration.class,
		ServletWebSecurityAutoConfiguration.class
})
abstract class ControllerSliceTestSupport {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	CustomerService customerService;

	@MockitoBean
	InvoiceService invoiceService;

	@MockitoBean
	ReportService reportService;
}
