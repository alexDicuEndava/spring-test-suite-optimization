package com.example.test_presentation.bookkeeping.dto;

import java.math.BigDecimal;

public record CustomerInvoiceSummaryResponse(
		Long customerId,
		long invoiceCount,
		BigDecimal totalInvoiced,
		BigDecimal openBalance,
		BigDecimal overdueBalance) {
}
