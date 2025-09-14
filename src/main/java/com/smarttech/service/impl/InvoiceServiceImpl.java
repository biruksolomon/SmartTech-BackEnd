package com.smarttech.service.impl;

import com.smarttech.entity.Invoice;
import com.smarttech.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl {

    private final InvoiceRepository invoiceRepository;

    public String getInvoicePdfUrl(String invoiceNumber) {
        return invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .map(Invoice::getPdfUrl)
                .orElse(null);
    }

    public String getInvoicePdfUrlByOrderNumber(String orderNumber) {
        return invoiceRepository.findByOrder_OrderNumber(orderNumber)
                .map(Invoice::getPdfUrl)
                .orElse(null);
    }
}
