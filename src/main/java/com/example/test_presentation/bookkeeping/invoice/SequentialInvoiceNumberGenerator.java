package com.example.test_presentation.bookkeeping.invoice;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

@Component
class SequentialInvoiceNumberGenerator implements InvoiceNumberGenerator {

	private final AtomicLong sequence = new AtomicLong(1000);

	@Override
	public String nextInvoiceNumber() {
		return "INV-%06d".formatted(sequence.incrementAndGet());
	}
}
