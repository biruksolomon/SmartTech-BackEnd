package com.smarttech.service.impl;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.smarttech.config.StorageProperties;
import com.smarttech.entity.*;
import com.smarttech.repository.InvoiceRepository;
import com.smarttech.repository.MaintenanceTicketRepository;
import com.smarttech.service.StorageService;
import com.smarttech.service.PdfGenerationService;
import com.smarttech.util.MaintenanceNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PdfGenerationServiceImpl implements PdfGenerationService {

    private final StorageService storageService;
    private final StorageProperties storageProperties;
    private final InvoiceRepository invoiceRepository;
    private final MaintenanceTicketRepository maintenanceTicketRepository;

    @Value("${business.name}")
    private String businessName;

    @Value("${business.logo-url}")
    private String logoUrl;

    @Override
    public String generateInvoicePdf(Order order) {
        log.info("Generating invoice PDF for order: {} using {} storage",
                order.getOrderNumber(), storageService.getStorageType());

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);

            document.open();

            // Add company header
            addCompanyHeader(document);

            // Add invoice details
            addInvoiceDetails(document, order);

            // Add customer details
            addCustomerDetails(document, order);

            // Add order items table
            addOrderItemsTable(document, order);

            // Add totals
            addOrderTotals(document, order);

            // Add footer
            addInvoiceFooter(document);

            document.close();

            byte[] pdfBytes = baos.toByteArray();
            String fileName = "invoice_" + order.getOrderNumber() + "_" + UUID.randomUUID().toString() + ".pdf";
            String folder = storageProperties.getFolders().getOrDefault("invoices", "invoices");

            String pdfUrl = storageService.uploadPdfBytes(pdfBytes, fileName, folder);

            log.info("Invoice PDF generated and uploaded successfully for order: {} to {}",
                    order.getOrderNumber(), pdfUrl);
            return pdfUrl;

        } catch (Exception e) {
            log.error("Failed to generate invoice PDF for order: {}", order.getOrderNumber(), e);
            throw new RuntimeException("Failed to generate invoice PDF", e);
        }
    }

    @Override
    public String generateMaintenanceTicketPdf(MaintenanceRequest maintenanceRequest) {
        log.info("Generating maintenance ticket PDF for request: {} using {} storage",
                maintenanceRequest.getRequestNumber(), storageService.getStorageType());

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);

            document.open();

            // Add company header
            addCompanyHeader(document);

            // Add ticket details
            addMaintenanceTicketDetails(document, maintenanceRequest);

            // Add customer details
            addMaintenanceCustomerDetails(document, maintenanceRequest);

            // Add device and issue details
            addDeviceDetails(document, maintenanceRequest);

            // Add service details
            addServiceDetails(document, maintenanceRequest);

            // Add footer
            addMaintenanceTicketFooter(document);

            document.close();

            byte[] pdfBytes = baos.toByteArray();
            String fileName = "ticket_" + maintenanceRequest.getRequestNumber() + "_" + UUID.randomUUID().toString() + ".pdf";
            String folder = storageProperties.getFolders().getOrDefault("maintenance", "maintenance");

            String pdfUrl = storageService.uploadPdfBytes(pdfBytes, fileName, folder);

            log.info("Maintenance ticket PDF generated and uploaded successfully for request: {} to {}",
                    maintenanceRequest.getRequestNumber(), pdfUrl);
            return pdfUrl;

        } catch (Exception e) {
            log.error("Failed to generate maintenance ticket PDF for request: {}", maintenanceRequest.getRequestNumber(), e);
            throw new RuntimeException("Failed to generate maintenance ticket PDF", e);
        }
    }

    @Override
    public Invoice createInvoiceRecord(Order order, String pdfUrl) {
        String invoiceNumber = "INV-" + order.getOrderNumber();

        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .order(order)
                .pdfUrl(pdfUrl)
                .build();

        return invoiceRepository.save(invoice);
    }

    @Override
    public MaintenanceTicket createMaintenanceTicketRecord(MaintenanceRequest maintenanceRequest, String pdfUrl) {
        String ticketNumber = MaintenanceNumberGenerator.generateTicketNumber();

        MaintenanceTicket ticket = MaintenanceTicket.builder()
                .ticketNumber(ticketNumber)
                .maintenanceRequest(maintenanceRequest)
                .pdfUrl(pdfUrl)
                .build();

        return maintenanceTicketRepository.save(ticket);
    }

    private void addCompanyHeader(Document document) throws DocumentException {
        try {
            String logoPath = "public/placeholder-logo.png";
            java.io.File logoFile = new java.io.File(logoPath);

            if (logoFile.exists()) {
                Image logo = Image.getInstance(logoPath);
                logo.scaleToFit(100, 50);
                logo.setAlignment(Element.ALIGN_CENTER);
                document.add(logo);
                document.add(new Paragraph(" "));
            }
        } catch (Exception e) {
            log.warn("Could not load logo image: {}", e.getMessage());
        }

        // Company name
        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD, Color.BLUE);
        Paragraph title = new Paragraph(businessName, titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        // Add some space
        document.add(new Paragraph(" "));
    }

    private void addInvoiceDetails(Document document, Order order) throws DocumentException {
        Font headerFont = new Font(Font.HELVETICA, 14, Font.BOLD);
        Font normalFont = new Font(Font.HELVETICA, 12);

        Paragraph invoiceTitle = new Paragraph("INVOICE", headerFont);
        invoiceTitle.setAlignment(Element.ALIGN_CENTER);
        document.add(invoiceTitle);
        document.add(new Paragraph(" "));

        // Invoice details table
        PdfPTable detailsTable = new PdfPTable(2);
        detailsTable.setWidthPercentage(100);

        addTableCell(detailsTable, "Invoice Number:", "INV-" + order.getOrderNumber(), headerFont, normalFont);
        addTableCell(detailsTable, "Order Number:", order.getOrderNumber(), headerFont, normalFont);
        addTableCell(detailsTable, "Date:", order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), headerFont, normalFont);
        addTableCell(detailsTable, "Status:", order.getStatus().toString(), headerFont, normalFont);

        document.add(detailsTable);
        document.add(new Paragraph(" "));
    }

    private void addCustomerDetails(Document document, Order order) throws DocumentException {
        Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD);
        Font normalFont = new Font(Font.HELVETICA, 10);

        Paragraph customerTitle = new Paragraph("CUSTOMER DETAILS", headerFont);
        document.add(customerTitle);

        User customer = order.getCustomer();
        document.add(new Paragraph("Name: " + customer.getFirstName() + " " + customer.getLastName(), normalFont));
        document.add(new Paragraph("Email: " + customer.getEmail(), normalFont));
        document.add(new Paragraph("Phone: " + customer.getPhoneNumber(), normalFont));
        if (order.getShippingAddress() != null) {
            document.add(new Paragraph("Address: " + order.getShippingAddress(), normalFont));
        }
        document.add(new Paragraph(" "));
    }

    private void addOrderItemsTable(Document document, Order order) throws DocumentException {
        Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD);
        Font normalFont = new Font(Font.HELVETICA, 9);

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 1, 2, 2, 2});

        // Headers
        addTableHeader(table, "Product", headerFont);
        addTableHeader(table, "Qty", headerFont);
        addTableHeader(table, "Unit Price", headerFont);
        addTableHeader(table, "Total", headerFont);
        addTableHeader(table, "Serial No.", headerFont);

        // Items
        for (OrderItem item : order.getOrderItems()) {
            addTableCell(table, item.getProduct().getName(), normalFont);
            addTableCell(table, item.getQuantity().toString(), normalFont);
            addTableCell(table, "ETB " + item.getUnitPrice().toString(), normalFont);
            addTableCell(table, "ETB " + item.getTotalPrice().toString(), normalFont);
            addTableCell(table, item.getSerialNumber() != null ? item.getSerialNumber() : "N/A", normalFont);
        }

        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void addOrderTotals(Document document, Order order) throws DocumentException {
        Font normalFont = new Font(Font.HELVETICA, 10);
        Font boldFont = new Font(Font.HELVETICA, 10, Font.BOLD);

        PdfPTable totalsTable = new PdfPTable(2);
        totalsTable.setWidthPercentage(50);
        totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

        addTableCell(totalsTable, "Subtotal:", "ETB " + order.getSubtotal().toString(), normalFont, normalFont);
        addTableCell(totalsTable, "VAT (15%):", "ETB " + order.getVatAmount().toString(), normalFont, normalFont);
        addTableCell(totalsTable, "Total Amount:", "ETB " + order.getTotalAmount().toString(), boldFont, boldFont);

        document.add(totalsTable);
    }

    private void addInvoiceFooter(Document document) throws DocumentException {
        Font footerFont = new Font(Font.HELVETICA, 8);

        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));

        Paragraph footer = new Paragraph("Thank you for your business!", footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        Paragraph terms = new Paragraph("Terms: Payment due within 30 days. Late payments may incur additional charges.", footerFont);
        terms.setAlignment(Element.ALIGN_CENTER);
        document.add(terms);
    }

    private void addMaintenanceTicketDetails(Document document, MaintenanceRequest request) throws DocumentException {
        Font headerFont = new Font(Font.HELVETICA, 14, Font.BOLD);
        Font normalFont = new Font(Font.HELVETICA, 12);

        Paragraph ticketTitle = new Paragraph("MAINTENANCE TICKET", headerFont);
        ticketTitle.setAlignment(Element.ALIGN_CENTER);
        document.add(ticketTitle);
        document.add(new Paragraph(" "));

        // Generate ticket number if not exists
        String ticketNumber = request.getMaintenanceTicket() != null ?
                request.getMaintenanceTicket().getTicketNumber() :
                MaintenanceNumberGenerator.generateTicketNumber();

        PdfPTable detailsTable = new PdfPTable(2);
        detailsTable.setWidthPercentage(100);

        addTableCell(detailsTable, "Ticket Number:", ticketNumber, headerFont, normalFont);
        addTableCell(detailsTable, "Request Number:", request.getRequestNumber(), headerFont, normalFont);
        addTableCell(detailsTable, "Date:", request.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), headerFont, normalFont);
        addTableCell(detailsTable, "Status:", request.getStatus().toString(), headerFont, normalFont);

        document.add(detailsTable);
        document.add(new Paragraph(" "));
    }

    private void addMaintenanceCustomerDetails(Document document, MaintenanceRequest request) throws DocumentException {
        Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD);
        Font normalFont = new Font(Font.HELVETICA, 10);

        Paragraph customerTitle = new Paragraph("CUSTOMER DETAILS", headerFont);
        document.add(customerTitle);

        User customer = request.getCustomer();
        document.add(new Paragraph("Name: " + customer.getFirstName() + " " + customer.getLastName(), normalFont));
        document.add(new Paragraph("Email: " + customer.getEmail(), normalFont));
        document.add(new Paragraph("Phone: " + customer.getPhoneNumber(), normalFont));
        document.add(new Paragraph(" "));
    }

    private void addDeviceDetails(Document document, MaintenanceRequest request) throws DocumentException {
        Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD);
        Font normalFont = new Font(Font.HELVETICA, 10);

        Paragraph deviceTitle = new Paragraph("DEVICE DETAILS", headerFont);
        document.add(deviceTitle);

        document.add(new Paragraph("Device Type: " + request.getDeviceType(), normalFont));
        if (request.getDeviceModel() != null) {
            document.add(new Paragraph("Model: " + request.getDeviceModel(), normalFont));
        }
        if (request.getSerialNumber() != null) {
            document.add(new Paragraph("Serial Number: " + request.getSerialNumber(), normalFont));
        }
        document.add(new Paragraph("Issue Description: " + request.getIssueDescription(), normalFont));
        document.add(new Paragraph(" "));
    }

    private void addServiceDetails(Document document, MaintenanceRequest request) throws DocumentException {
        Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD);
        Font normalFont = new Font(Font.HELVETICA, 10);

        Paragraph serviceTitle = new Paragraph("SERVICE DETAILS", headerFont);
        document.add(serviceTitle);

        document.add(new Paragraph("Service Type: " + request.getMaintenanceType().toString(), normalFont));
        document.add(new Paragraph("Warranty Covered: " + (request.getIsWarrantyCovered() ? "Yes" : "No"), normalFont));

        if (request.getEstimatedCost() != null) {
            document.add(new Paragraph("Estimated Cost: ETB " + request.getEstimatedCost().toString(), normalFont));
        }

        if (request.getEstimatedCompletionDate() != null) {
            document.add(new Paragraph("Estimated Completion: " +
                    request.getEstimatedCompletionDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), normalFont));
        }

        if (request.getAdminNotes() != null) {
            document.add(new Paragraph("Notes: " + request.getAdminNotes(), normalFont));
        }

        document.add(new Paragraph(" "));
    }

    private void addMaintenanceTicketFooter(Document document) throws DocumentException {
        Font footerFont = new Font(Font.HELVETICA, 8);

        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));

        Paragraph footer = new Paragraph("Please keep this ticket for your records.", footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);

        Paragraph contact = new Paragraph("For inquiries, please contact us with your ticket number.", footerFont);
        contact.setAlignment(Element.ALIGN_CENTER);
        document.add(contact);
    }

    private void addTableCell(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(valueCell);
    }

    private void addTableHeader(PdfPTable table, String header, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(header, font));
        cell.setBackgroundColor(Color.LIGHT_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String content, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }
}
