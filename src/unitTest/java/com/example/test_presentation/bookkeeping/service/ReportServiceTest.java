package com.example.test_presentation.bookkeeping.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import com.example.test_presentation.bookkeeping.dto.InvoiceAgingReportResponse;
import com.example.test_presentation.bookkeeping.model.Customer;
import com.example.test_presentation.bookkeeping.model.CustomerStatus;
import com.example.test_presentation.bookkeeping.model.Invoice;
import com.example.test_presentation.bookkeeping.model.InvoiceStatus;
import com.example.test_presentation.bookkeeping.repository.InvoiceRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class ReportServiceTest {

	private final InvoiceRepository invoiceRepository = Mockito.mock(InvoiceRepository.class);
	private final ReportService reportService = new ReportService(invoiceRepository);

	@ParameterizedTest(name = "ages {2} as of {0}")
	@MethodSource("agingCases")
	void calculatesAgingBuckets(LocalDate asOf, int row, String customerName, long invoiceCount, String current,
			String oneToThirty, String thirtyOneToSixty, String sixtyOnePlus, String openBalance) {
		given(invoiceRepository.findOpenInvoicesForAgingReport(List.of(InvoiceStatus.PAID, InvoiceStatus.VOID)))
				.willReturn(reportInvoices());

		List<InvoiceAgingReportResponse> report = reportService.invoiceAging(asOf);

		assertThat(report).hasSize(3);
		InvoiceAgingReportResponse response = report.get(row);
		assertThat(response.customerName()).isEqualTo(customerName);
		assertThat(response.invoiceCount()).isEqualTo(invoiceCount);
		assertThat(response.current()).isEqualByComparingTo(current);
		assertThat(response.oneToThirty()).isEqualByComparingTo(oneToThirty);
		assertThat(response.thirtyOneToSixty()).isEqualByComparingTo(thirtyOneToSixty);
		assertThat(response.sixtyOnePlus()).isEqualByComparingTo(sixtyOnePlus);
		assertThat(response.openBalance()).isEqualByComparingTo(openBalance);
		verify(invoiceRepository).findOpenInvoicesForAgingReport(List.of(InvoiceStatus.PAID, InvoiceStatus.VOID));
	}

	@Test
	void returnsEmptyReportWhenNoOpenInvoicesExist() {
		given(invoiceRepository.findOpenInvoicesForAgingReport(List.of(InvoiceStatus.PAID, InvoiceStatus.VOID)))
				.willReturn(List.of());

		assertThat(reportService.invoiceAging(LocalDate.parse("2026-06-22"))).isEmpty();
	}

	static Stream<Arguments> agingCases() {
		return Stream.of(
				Arguments.of(LocalDate.parse("2026-03-02"), 0, "Acme Books", 4, "260.00", "40.00", "0.00", "0.00",
						"300.00"),
				Arguments.of(LocalDate.parse("2026-03-02"), 1, "Beacon Retail", 3, "300.00", "0.00", "0.00", "0.00",
						"300.00"),
				Arguments.of(LocalDate.parse("2026-03-02"), 2, "Cobalt Services", 3, "66.00", "0.00", "0.00", "0.00",
						"66.00"),
				Arguments.of(LocalDate.parse("2026-04-23"), 0, "Acme Books", 4, "260.00", "0.00", "40.00", "0.00",
						"300.00"),
				Arguments.of(LocalDate.parse("2026-04-23"), 1, "Beacon Retail", 3, "300.00", "0.00", "0.00", "0.00",
						"300.00"),
				Arguments.of(LocalDate.parse("2026-04-23"), 2, "Cobalt Services", 3, "33.00", "33.00", "0.00", "0.00",
						"66.00"),
				Arguments.of(LocalDate.parse("2026-05-01"), 0, "Acme Books", 4, "260.00", "0.00", "0.00", "40.00",
						"300.00"),
				Arguments.of(LocalDate.parse("2026-05-01"), 1, "Beacon Retail", 3, "230.00", "70.00", "0.00", "0.00",
						"300.00"),
				Arguments.of(LocalDate.parse("2026-05-01"), 2, "Cobalt Services", 3, "33.00", "33.00", "0.00", "0.00",
						"66.00"),
				Arguments.of(LocalDate.parse("2026-06-01"), 0, "Acme Books", 4, "200.00", "0.00", "60.00", "40.00",
						"300.00"),
				Arguments.of(LocalDate.parse("2026-06-01"), 1, "Beacon Retail", 3, "230.00", "0.00", "70.00", "0.00",
						"300.00"),
				Arguments.of(LocalDate.parse("2026-06-01"), 2, "Cobalt Services", 3, "11.00", "22.00", "33.00", "0.00",
						"66.00"),
				Arguments.of(LocalDate.parse("2026-06-21"), 0, "Acme Books", 4, "120.00", "80.00", "60.00", "40.00",
						"300.00"),
				Arguments.of(LocalDate.parse("2026-06-21"), 1, "Beacon Retail", 3, "230.00", "0.00", "70.00", "0.00",
						"300.00"),
				Arguments.of(LocalDate.parse("2026-06-21"), 2, "Cobalt Services", 3, "0.00", "33.00", "0.00", "33.00",
						"66.00"),
				Arguments.of(LocalDate.parse("2026-06-22"), 0, "Acme Books", 4, "120.00", "80.00", "60.00", "40.00",
						"300.00"),
				Arguments.of(LocalDate.parse("2026-06-22"), 1, "Beacon Retail", 3, "200.00", "30.00", "70.00", "0.00",
						"300.00"),
				Arguments.of(LocalDate.parse("2026-06-22"), 2, "Cobalt Services", 3, "0.00", "11.00", "22.00", "33.00",
						"66.00"),
				Arguments.of(LocalDate.parse("2026-07-22"), 0, "Acme Books", 4, "0.00", "120.00", "80.00", "100.00",
						"300.00"),
				Arguments.of(LocalDate.parse("2026-07-22"), 1, "Beacon Retail", 3, "0.00", "200.00", "30.00", "70.00",
						"300.00"),
				Arguments.of(LocalDate.parse("2026-07-22"), 2, "Cobalt Services", 3, "0.00", "0.00", "11.00", "55.00",
						"66.00"));
	}

	private static List<Invoice> reportInvoices() {
		Customer acme = customer(300L, "Acme Books", CustomerStatus.ACTIVE);
		Customer beacon = customer(301L, "Beacon Retail", CustomerStatus.ACTIVE);
		Customer cobalt = customer(302L, "Cobalt Services", CustomerStatus.INACTIVE);
		return List.of(
				invoice("INV-AGE-001", acme, InvoiceStatus.SENT, "2026-06-22", "120.00"),
				invoice("INV-AGE-002", acme, InvoiceStatus.DRAFT, "2026-06-01", "80.00"),
				invoice("INV-AGE-003", acme, InvoiceStatus.SENT, "2026-05-01", "60.00"),
				invoice("INV-AGE-004", acme, InvoiceStatus.DRAFT, "2026-03-01", "40.00"),
				invoice("INV-AGE-007", beacon, InvoiceStatus.SENT, "2026-06-23", "200.00"),
				invoice("INV-AGE-008", beacon, InvoiceStatus.SENT, "2026-06-21", "30.00"),
				invoice("INV-AGE-009", beacon, InvoiceStatus.DRAFT, "2026-04-23", "70.00"),
				invoice("INV-AGE-010", cobalt, InvoiceStatus.SENT, "2026-06-15", "11.00"),
				invoice("INV-AGE-011", cobalt, InvoiceStatus.DRAFT, "2026-05-22", "22.00"),
				invoice("INV-AGE-012", cobalt, InvoiceStatus.SENT, "2026-04-21", "33.00"));
	}

	private static Customer customer(Long id, String name, CustomerStatus status) {
		Customer customer = new Customer(name, name.toLowerCase().replace(" ", "-") + "@example.test", status);
		ReflectionTestUtils.setField(customer, "id", id);
		return customer;
	}

	private static Invoice invoice(String number, Customer customer, InvoiceStatus status, String dueDate, String total) {
		return new Invoice(number, customer, status, LocalDate.parse("2026-01-15"), LocalDate.parse(dueDate),
				new BigDecimal(total));
	}
}
