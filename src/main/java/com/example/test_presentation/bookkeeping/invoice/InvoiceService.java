package com.example.test_presentation.bookkeeping.invoice;

import java.util.List;
import java.util.Optional;

import com.example.test_presentation.bookkeeping.common.ResourceNotFoundException;
import com.example.test_presentation.bookkeeping.customer.Customer;
import com.example.test_presentation.bookkeeping.customer.CustomerRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InvoiceService {

	private final InvoiceRepository invoiceRepository;
	private final CustomerRepository customerRepository;
	private final InvoiceNumberGenerator invoiceNumberGenerator;

	public InvoiceService(InvoiceRepository invoiceRepository, CustomerRepository customerRepository,
			InvoiceNumberGenerator invoiceNumberGenerator) {
		this.invoiceRepository = invoiceRepository;
		this.customerRepository = customerRepository;
		this.invoiceNumberGenerator = invoiceNumberGenerator;
	}

	public InvoiceResponse create(InvoiceRequest request) {
		Customer customer = customerRepository.findById(request.customerId())
				.orElseThrow(() -> new ResourceNotFoundException("Customer %d was not found".formatted(request.customerId())));
		Invoice invoice = new Invoice(
				invoiceNumberGenerator.nextInvoiceNumber(),
				customer,
				request.status(),
				request.issueDate(),
				request.dueDate(),
				request.total());
		return InvoiceResponse.from(invoiceRepository.save(invoice));
	}

	@Transactional(readOnly = true)
	public InvoiceResponse find(Long id) {
		return InvoiceResponse.from(findEntity(id));
	}

	@Transactional(readOnly = true)
	public List<InvoiceResponse> findAll(Optional<InvoiceStatus> status) {
		List<Invoice> invoices = status.map(invoiceRepository::findByStatus)
				.orElseGet(invoiceRepository::findAll);
		return invoices.stream().map(InvoiceResponse::from).toList();
	}

	public InvoiceResponse update(Long id, InvoiceRequest request) {
		Invoice invoice = findEntity(id);
		invoice.setStatus(request.status());
		invoice.setIssueDate(request.issueDate());
		invoice.setDueDate(request.dueDate());
		invoice.setTotal(request.total());
		return InvoiceResponse.from(invoice);
	}

	public void delete(Long id) {
		if (!invoiceRepository.existsById(id)) {
			throw new ResourceNotFoundException("Invoice %d was not found".formatted(id));
		}
		invoiceRepository.deleteById(id);
	}

	private Invoice findEntity(Long id) {
		return invoiceRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Invoice %d was not found".formatted(id)));
	}
}
