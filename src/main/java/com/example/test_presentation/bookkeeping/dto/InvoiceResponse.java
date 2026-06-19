package com.example.test_presentation.bookkeeping.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.test_presentation.bookkeeping.model.Invoice;
import com.example.test_presentation.bookkeeping.model.InvoiceStatus;

public record InvoiceResponse(
		Long id,
		String invoiceNumber,
		Long customerId,
		InvoiceStatus status,
		LocalDate issueDate,
		LocalDate dueDate,
		BigDecimal total) {

	public static InvoiceResponse from(Invoice invoice) {
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
