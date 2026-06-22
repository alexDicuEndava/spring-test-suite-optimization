package com.example.test_presentation.bookkeeping.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.test_presentation.bookkeeping.dto.InvoiceAgingReportResponse;
import com.example.test_presentation.bookkeeping.model.Invoice;
import com.example.test_presentation.bookkeeping.model.InvoiceStatus;
import com.example.test_presentation.bookkeeping.repository.InvoiceRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportService {

	private final InvoiceRepository invoiceRepository;

	public ReportService(InvoiceRepository invoiceRepository) {
		this.invoiceRepository = invoiceRepository;
	}

	public List<InvoiceAgingReportResponse> invoiceAging(LocalDate asOf) {
		List<Invoice> invoices = invoiceRepository.findOpenInvoicesForAgingReport(
				List.of(InvoiceStatus.PAID, InvoiceStatus.VOID));
		Map<Long, AgingTotals> totalsByCustomer = new LinkedHashMap<>();
		for (Invoice invoice : invoices) {
			totalsByCustomer.computeIfAbsent(invoice.getCustomer().getId(),
					id -> new AgingTotals(id, invoice.getCustomer().getName()))
					.add(invoice, asOf);
		}
		return totalsByCustomer.values().stream().map(AgingTotals::toResponse).toList();
	}

	private static final class AgingTotals {

		private final Long customerId;
		private final String customerName;
		private long invoiceCount;
		private BigDecimal current = BigDecimal.ZERO;
		private BigDecimal oneToThirty = BigDecimal.ZERO;
		private BigDecimal thirtyOneToSixty = BigDecimal.ZERO;
		private BigDecimal sixtyOnePlus = BigDecimal.ZERO;

		private AgingTotals(Long customerId, String customerName) {
			this.customerId = customerId;
			this.customerName = customerName;
		}

		private void add(Invoice invoice, LocalDate asOf) {
			invoiceCount++;
			long daysOverdue = ChronoUnit.DAYS.between(invoice.getDueDate(), asOf);
			if (daysOverdue <= 0) {
				current = current.add(invoice.getTotal());
			}
			else if (daysOverdue <= 30) {
				oneToThirty = oneToThirty.add(invoice.getTotal());
			}
			else if (daysOverdue <= 60) {
				thirtyOneToSixty = thirtyOneToSixty.add(invoice.getTotal());
			}
			else {
				sixtyOnePlus = sixtyOnePlus.add(invoice.getTotal());
			}
		}

		private InvoiceAgingReportResponse toResponse() {
			return new InvoiceAgingReportResponse(customerId, customerName, invoiceCount, current, oneToThirty,
					thirtyOneToSixty, sixtyOnePlus, openBalance());
		}

		private BigDecimal openBalance() {
			return current.add(oneToThirty).add(thirtyOneToSixty).add(sixtyOnePlus);
		}
	}
}
