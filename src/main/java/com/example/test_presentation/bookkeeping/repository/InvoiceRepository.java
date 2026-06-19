package com.example.test_presentation.bookkeeping.repository;

import java.util.List;

import com.example.test_presentation.bookkeeping.model.Invoice;
import com.example.test_presentation.bookkeeping.model.InvoiceStatus;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

	List<Invoice> findByStatus(InvoiceStatus status);

	List<Invoice> findByCustomerId(Long customerId);
}
