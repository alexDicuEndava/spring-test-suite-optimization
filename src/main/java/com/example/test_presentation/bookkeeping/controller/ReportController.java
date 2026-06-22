package com.example.test_presentation.bookkeeping.controller;

import java.time.LocalDate;
import java.util.List;

import com.example.test_presentation.bookkeeping.dto.InvoiceAgingReportResponse;
import com.example.test_presentation.bookkeeping.service.ReportService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
class ReportController {

	private final ReportService reportService;

	ReportController(ReportService reportService) {
		this.reportService = reportService;
	}

	@GetMapping("/aging")
	List<InvoiceAgingReportResponse> invoiceAging(@RequestParam LocalDate asOf) {
		return reportService.invoiceAging(asOf);
	}
}
