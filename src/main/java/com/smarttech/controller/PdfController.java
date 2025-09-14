package com.smarttech.controller;

import com.smarttech.service.PdfGenerationService;
import com.smarttech.service.impl.InvoiceServiceImpl;
import com.smarttech.service.impl.MaintenanceServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/pdf")
@RequiredArgsConstructor
@Tag(name = "PDF Downloads", description = "PDF download APIs for invoices and maintenance tickets")
public class PdfController {

    private final InvoiceServiceImpl invoiceService;
    private final MaintenanceServiceImpl maintenanceService;

    @GetMapping("/invoice/{invoiceNumber}")
    @Operation(summary = "Download invoice PDF", description = "Download invoice PDF by invoice number")
    public ResponseEntity<Resource> downloadInvoicePdf(@PathVariable String invoiceNumber) {
        try {
            String pdfUrl = invoiceService.getInvoicePdfUrl(invoiceNumber);

            if (pdfUrl == null) {
                return ResponseEntity.notFound().build();
            }

            // For S3 URLs, redirect to the URL
            if (pdfUrl.startsWith("http")) {
                return ResponseEntity.status(302)
                        .header(HttpHeaders.LOCATION, pdfUrl)
                        .build();
            }

            // For local files
            Path filePath = Paths.get(pdfUrl);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + invoiceNumber + ".pdf\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/ticket/{ticketNumber}")
    @Operation(summary = "Download maintenance ticket PDF", description = "Download maintenance ticket PDF by ticket number")
    public ResponseEntity<Resource> downloadMaintenanceTicketPdf(@PathVariable String ticketNumber) {
        try {
            String pdfUrl = maintenanceService.getMaintenanceTicketPdfUrl(ticketNumber);

            if (pdfUrl == null) {
                return ResponseEntity.notFound().build();
            }

            // For S3 URLs, redirect to the URL
            if (pdfUrl.startsWith("http")) {
                return ResponseEntity.status(302)
                        .header(HttpHeaders.LOCATION, pdfUrl)
                        .build();
            }

            // For local files
            Path filePath = Paths.get(pdfUrl);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + ticketNumber + ".pdf\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/invoice/order/{orderNumber}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Download invoice PDF by order number", description = "Download invoice PDF by order number (Admin only)")
    public ResponseEntity<Resource> downloadInvoicePdfByOrder(@PathVariable String orderNumber) {
        try {
            String pdfUrl = invoiceService.getInvoicePdfUrlByOrderNumber(orderNumber);

            if (pdfUrl == null) {
                return ResponseEntity.notFound().build();
            }

            if (pdfUrl.startsWith("http")) {
                return ResponseEntity.status(302)
                        .header(HttpHeaders.LOCATION, pdfUrl)
                        .build();
            }

            Path filePath = Paths.get(pdfUrl);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"invoice_" + orderNumber + ".pdf\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
