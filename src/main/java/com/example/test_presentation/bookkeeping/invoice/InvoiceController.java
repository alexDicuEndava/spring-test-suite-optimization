package com.example.test_presentation.bookkeeping.invoice;

import java.util.List;
import java.util.Optional;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices")
class InvoiceController {

	private final InvoiceService invoiceService;

	InvoiceController(InvoiceService invoiceService) {
		this.invoiceService = invoiceService;
	}

	@GetMapping
	List<InvoiceResponse> list(@RequestParam Optional<InvoiceStatus> status) {
		return invoiceService.findAll(status);
	}

	@GetMapping("/{id}")
	InvoiceResponse find(@PathVariable Long id) {
		return invoiceService.find(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	InvoiceResponse create(@Valid @RequestBody InvoiceRequest request) {
		return invoiceService.create(request);
	}

	@PutMapping("/{id}")
	InvoiceResponse update(@PathVariable Long id, @Valid @RequestBody InvoiceRequest request) {
		return invoiceService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable Long id) {
		invoiceService.delete(id);
	}
}
