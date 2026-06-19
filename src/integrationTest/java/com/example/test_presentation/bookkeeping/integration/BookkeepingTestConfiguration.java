package com.example.test_presentation.bookkeeping.integration;

import java.util.concurrent.atomic.AtomicInteger;

import com.example.test_presentation.bookkeeping.service.InvoiceNumberGenerator;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
class BookkeepingTestConfiguration {

	@Bean
	@Primary
	InvoiceNumberGenerator deterministicInvoiceNumberGenerator() {
		AtomicInteger sequence = new AtomicInteger();
		return () -> "INV-TEST-%03d".formatted(sequence.incrementAndGet());
	}
}
