package com.example.test_presentation.bookkeeping.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.test_presentation.bookkeeping.customer.Customer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "invoices")
public class Invoice {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String invoiceNumber;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false)
	private Customer customer;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private InvoiceStatus status;

	@Column(nullable = false)
	private LocalDate issueDate;

	@Column(nullable = false)
	private LocalDate dueDate;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal total;

	protected Invoice() {
	}

	public Invoice(String invoiceNumber, Customer customer, InvoiceStatus status, LocalDate issueDate,
			LocalDate dueDate, BigDecimal total) {
		this.invoiceNumber = invoiceNumber;
		this.customer = customer;
		this.status = status;
		this.issueDate = issueDate;
		this.dueDate = dueDate;
		this.total = total;
	}

	public Long getId() {
		return id;
	}

	public String getInvoiceNumber() {
		return invoiceNumber;
	}

	public Customer getCustomer() {
		return customer;
	}

	public InvoiceStatus getStatus() {
		return status;
	}

	public void setStatus(InvoiceStatus status) {
		this.status = status;
	}

	public LocalDate getIssueDate() {
		return issueDate;
	}

	public void setIssueDate(LocalDate issueDate) {
		this.issueDate = issueDate;
	}

	public LocalDate getDueDate() {
		return dueDate;
	}

	public void setDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
	}

	public BigDecimal getTotal() {
		return total;
	}

	public void setTotal(BigDecimal total) {
		this.total = total;
	}
}
