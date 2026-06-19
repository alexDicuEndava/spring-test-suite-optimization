package com.example.test_presentation.bookkeeping.service;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.test_presentation.bookkeeping.dto.CustomerInvoiceSummaryResponse;
import com.example.test_presentation.bookkeeping.dto.CustomerRequest;
import com.example.test_presentation.bookkeeping.dto.CustomerResponse;
import com.example.test_presentation.bookkeeping.exception.ResourceNotFoundException;
import com.example.test_presentation.bookkeeping.model.Customer;
import com.example.test_presentation.bookkeeping.model.CustomerStatus;
import com.example.test_presentation.bookkeeping.model.Invoice;
import com.example.test_presentation.bookkeeping.model.InvoiceStatus;
import com.example.test_presentation.bookkeeping.repository.CustomerRepository;
import com.example.test_presentation.bookkeeping.repository.InvoiceRepository;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CustomerService {

	private final CustomerRepository customerRepository;
	private final InvoiceRepository invoiceRepository;

	public CustomerService(CustomerRepository customerRepository, InvoiceRepository invoiceRepository) {
		this.customerRepository = customerRepository;
		this.invoiceRepository = invoiceRepository;
	}

	public CustomerResponse create(CustomerRequest request) {
		Customer customer = new Customer(request.name(), request.email(), request.status());
		return CustomerResponse.from(customerRepository.save(customer));
	}

	@Transactional(readOnly = true)
	@Cacheable(cacheNames = "customers", key = "#id", condition = "@bookkeepingCacheProperties.enabled")
	public CustomerResponse find(Long id) {
		return CustomerResponse.from(findEntity(id));
	}

	@Transactional(readOnly = true)
	public List<CustomerResponse> findAll(Optional<CustomerStatus> status) {
		List<Customer> customers = status.map(customerRepository::findByStatus)
				.orElseGet(customerRepository::findAll);
		return customers.stream().map(CustomerResponse::from).toList();
	}

	@CacheEvict(cacheNames = "customers", key = "#id", condition = "@bookkeepingCacheProperties.enabled")
	public CustomerResponse update(Long id, CustomerRequest request) {
		Customer customer = findEntity(id);
		customer.setName(request.name());
		customer.setEmail(request.email());
		customer.setStatus(request.status());
		return CustomerResponse.from(customer);
	}

	@CacheEvict(cacheNames = "customers", key = "#id", condition = "@bookkeepingCacheProperties.enabled")
	public void delete(Long id) {
		if (!customerRepository.existsById(id)) {
			throw new ResourceNotFoundException("Customer %d was not found".formatted(id));
		}
		customerRepository.deleteById(id);
	}

	@Transactional(readOnly = true)
	public CustomerInvoiceSummaryResponse invoiceSummary(Long customerId, Optional<LocalDate> overdueOn) {
		if (!customerRepository.existsById(customerId)) {
			throw new ResourceNotFoundException("Customer %d was not found".formatted(customerId));
		}
		LocalDate referenceDate = overdueOn.orElse(LocalDate.now());
		List<Invoice> invoices = invoiceRepository.findByCustomerId(customerId);
		BigDecimal totalInvoiced = sum(invoices);
		BigDecimal openBalance = sum(invoices.stream().filter(CustomerService::isOpen).toList());
		BigDecimal overdueBalance = sum(invoices.stream()
				.filter(CustomerService::isOpen)
				.filter(invoice -> invoice.getDueDate().isBefore(referenceDate))
				.toList());
		return new CustomerInvoiceSummaryResponse(customerId, invoices.size(), totalInvoiced, openBalance, overdueBalance);
	}

	private Customer findEntity(Long id) {
		return customerRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Customer %d was not found".formatted(id)));
	}

	private static boolean isOpen(Invoice invoice) {
		return invoice.getStatus() != InvoiceStatus.PAID && invoice.getStatus() != InvoiceStatus.VOID;
	}

	private static BigDecimal sum(List<Invoice> invoices) {
		return invoices.stream().map(Invoice::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}
