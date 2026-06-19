package com.example.test_presentation.bookkeeping.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoiceResponse(
		Long id,
		String invoiceNumber,
		Long customerId,
		InvoiceStatus status,
		LocalDate issueDate,
		LocalDate dueDate,
		BigDecimal total) {

	static InvoiceResponse from(Invoice invoice) {
		return new InvoiceResponse(
				invoice.getId(),
				invoice.getInvoiceNumber(),
				invoice.getCustomer().getId(),
				invoice.getStatus(),
				invoice.getIssueDate(),
				invoice.getDueDate(),
				invoice.getTotal());
	}
}
