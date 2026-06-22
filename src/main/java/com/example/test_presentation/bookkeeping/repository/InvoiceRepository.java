package com.example.test_presentation.bookkeeping.repository;

import java.util.List;
import java.time.LocalDate;

import com.example.test_presentation.bookkeeping.model.Invoice;
import com.example.test_presentation.bookkeeping.model.InvoiceStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

	List<Invoice> findByStatus(InvoiceStatus status);

	List<Invoice> findByCustomerId(Long customerId);

	@Query("""
			select i from Invoice i
			join fetch i.customer c
			where i.status not in :closedStatuses
			order by c.name, i.dueDate, i.id
			""")
	List<Invoice> findOpenInvoicesForAgingReport(@Param("closedStatuses") List<InvoiceStatus> closedStatuses);

	@Query("""
			select i from Invoice i
			where (:customerId is null or i.customer.id = :customerId)
				and (:status is null or i.status = :status)
				and (:overdueOn is null or (i.dueDate < :overdueOn and i.status not in :closedStatuses))
			order by i.id
			""")
	List<Invoice> search(
			@Param("customerId") Long customerId,
			@Param("status") InvoiceStatus status,
			@Param("overdueOn") LocalDate overdueOn,
			@Param("closedStatuses") List<InvoiceStatus> closedStatuses);
}
