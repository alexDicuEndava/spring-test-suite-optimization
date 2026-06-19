package com.example.test_presentation.bookkeeping.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record InvoiceRequest(
		@NotNull Long customerId,
		@NotNull InvoiceStatus status,
		@NotNull LocalDate issueDate,
		@NotNull LocalDate dueDate,
		@NotNull @DecimalMin("0.00") BigDecimal total) {
}
