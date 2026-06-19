package com.example.test_presentation.bookkeeping.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SequentialInvoiceNumberGeneratorUnitTest {

	@Test
	void generatesReadableInvoiceNumbers() {
		SequentialInvoiceNumberGenerator generator = new SequentialInvoiceNumberGenerator();

		assertThat(generator.nextInvoiceNumber()).isEqualTo("INV-001001");
	}

	@Test
	void incrementsNumbersWithoutSpringContext() {
		SequentialInvoiceNumberGenerator generator = new SequentialInvoiceNumberGenerator();

		generator.nextInvoiceNumber();

		assertThat(generator.nextInvoiceNumber()).isEqualTo("INV-001002");
	}
}
