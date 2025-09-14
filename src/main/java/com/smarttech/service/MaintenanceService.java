package com.smarttech.service;

import com.smarttech.dto.request.MaintenanceRequestCreate;
import com.smarttech.dto.response.MaintenanceRequestResponse;
import com.smarttech.dto.response.MaintenanceTicketResponse;
import com.smarttech.entity.MaintenanceRequest;
import com.smarttech.enums.MaintenanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface MaintenanceService {
    MaintenanceRequestResponse createMaintenanceRequest(MaintenanceRequestCreate request, Long customerId);
    MaintenanceRequestResponse getMaintenanceRequestById(Long id);
    MaintenanceRequestResponse getMaintenanceRequestByNumber(String requestNumber);
    Page<MaintenanceRequestResponse> getCustomerMaintenanceRequests(Long customerId, Pageable pageable);
    Page<MaintenanceRequestResponse> getAllMaintenanceRequests(Pageable pageable);
    List<MaintenanceRequestResponse> getPendingRequests();
    MaintenanceRequestResponse approveMaintenanceRequest(Long requestId, BigDecimal estimatedCost, 
                                                        String adminNotes, LocalDateTime estimatedCompletionDate);
    MaintenanceRequestResponse rejectMaintenanceRequest(Long requestId, String reason);
    MaintenanceRequestResponse updateMaintenanceStatus(Long requestId, MaintenanceStatus status);
    MaintenanceTicketResponse generateMaintenanceTicket(Long requestId);
    MaintenanceTicketResponse getMaintenanceTicketByNumber(String ticketNumber);
    List<MaintenanceRequestResponse> getOverdueRequests();
    MaintenanceRequest findEntityById(Long id);
}
