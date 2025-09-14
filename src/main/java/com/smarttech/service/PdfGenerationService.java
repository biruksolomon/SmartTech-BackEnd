package com.smarttech.service;

import com.smarttech.entity.Invoice;
import com.smarttech.entity.MaintenanceTicket;
import com.smarttech.entity.Order;
import com.smarttech.entity.MaintenanceRequest;

public interface PdfGenerationService {
    String generateInvoicePdf(Order order);
    String generateMaintenanceTicketPdf(MaintenanceRequest maintenanceRequest);
    Invoice createInvoiceRecord(Order order, String pdfUrl);
    MaintenanceTicket createMaintenanceTicketRecord(MaintenanceRequest maintenanceRequest, String pdfUrl);
}
