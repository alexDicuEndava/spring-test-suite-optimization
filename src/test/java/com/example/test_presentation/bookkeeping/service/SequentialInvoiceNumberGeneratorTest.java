package com.example.test_presentation.bookkeeping.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class SequentialInvoiceNumberGeneratorTest {

	@RepeatedTest(5)
	void startsAtExpectedNumberForEachInstance() {
		SequentialInvoiceNumberGenerator generator = new SequentialInvoiceNumberGenerator();

		assertThat(generator.nextInvoiceNumber()).isEqualTo("INV-001001");
	}

	@ParameterizedTest(name = "advances sequence {0} times")
	@ValueSource(ints = { 1, 2, 3, 4, 5 })
	void advancesSequence(int calls) {
		SequentialInvoiceNumberGenerator generator = new SequentialInvoiceNumberGenerator();
		String last = null;

		for (int i = 0; i < calls; i++) {
			last = generator.nextInvoiceNumber();
		}

		assertThat(last).isEqualTo("INV-%06d".formatted(1000 + calls));
	}
}
