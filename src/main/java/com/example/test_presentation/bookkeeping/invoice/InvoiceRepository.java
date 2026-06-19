package com.example.test_presentation.bookkeeping.invoice;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

	List<Invoice> findByStatus(InvoiceStatus status);

	List<Invoice> findByCustomerId(Long customerId);
}
