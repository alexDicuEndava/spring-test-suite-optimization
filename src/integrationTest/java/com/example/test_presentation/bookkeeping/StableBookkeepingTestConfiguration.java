package com.example.test_presentation.bookkeeping;

import java.util.concurrent.atomic.AtomicInteger;

import com.example.test_presentation.bookkeeping.invoice.InvoiceNumberGenerator;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
class StableBookkeepingTestConfiguration {

	@Bean
	@Primary
	InvoiceNumberGenerator deterministicInvoiceNumberGenerator() {
		AtomicInteger sequence = new AtomicInteger();
		return () -> "INV-TEST-%03d".formatted(sequence.incrementAndGet());
	}
}
