package com.smarttech.dto.response;

import com.smarttech.enums.MaintenanceStatus;
import com.smarttech.enums.MaintenanceType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MaintenanceRequestResponse {
    private Long id;
    private String requestNumber;
    private UserResponse customer;
    private String deviceType;
    private String deviceModel;
    private String serialNumber;
    private String issueDescription;
    private MaintenanceType maintenanceType;
    private MaintenanceStatus status;
    private List<String> imageUrls;
    private BigDecimal estimatedCost;
    private BigDecimal actualCost;
    private String adminNotes;
    private LocalDateTime estimatedCompletionDate;
    private LocalDateTime completedDate;
    private Boolean isWarrantyCovered;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private MaintenanceTicketResponse maintenanceTicket;
}
