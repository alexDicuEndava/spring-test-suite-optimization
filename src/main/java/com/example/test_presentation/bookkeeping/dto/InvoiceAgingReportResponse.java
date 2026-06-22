package com.example.test_presentation.bookkeeping.dto;

import java.math.BigDecimal;

public record InvoiceAgingReportResponse(
		Long customerId,
		String customerName,
		long invoiceCount,
		BigDecimal current,
		BigDecimal oneToThirty,
		BigDecimal thirtyOneToSixty,
		BigDecimal sixtyOnePlus,
		BigDecimal openBalance) {
}
