package com.smarttech.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MaintenanceTicketResponse {
    private Long id;
    private String ticketNumber;
    private String pdfUrl;
    private LocalDateTime createdAt;
}
