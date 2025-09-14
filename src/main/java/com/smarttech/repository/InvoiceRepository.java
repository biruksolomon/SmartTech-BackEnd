package com.smarttech.repository;

import com.smarttech.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    Optional<Invoice> findByOrderId(Long orderId);
    Optional<Invoice> findByOrder_OrderNumber(String orderNumber);
}
