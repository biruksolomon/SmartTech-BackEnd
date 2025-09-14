package com.smarttech.controller;

import com.smarttech.dto.request.MaintenanceRequestCreate;
import com.smarttech.dto.response.MaintenanceRequestResponse;
import com.smarttech.dto.response.MaintenanceTicketResponse;
import com.smarttech.enums.MaintenanceStatus;
import com.smarttech.security.UserPrincipal;
import com.smarttech.service.MaintenanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/maintenance")
@RequiredArgsConstructor
@Tag(name = "Maintenance", description = "Maintenance service management APIs")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    @PostMapping("/requests")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Create maintenance request", description = "Create new maintenance request (Customer only)")
    public ResponseEntity<MaintenanceRequestResponse> createMaintenanceRequest(
            @Valid @RequestBody MaintenanceRequestCreate request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        MaintenanceRequestResponse response = maintenanceService.createMaintenanceRequest(request, userPrincipal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/requests/{id}")
    @Operation(summary = "Get maintenance request by ID", description = "Retrieve maintenance request details by ID")
    public ResponseEntity<MaintenanceRequestResponse> getMaintenanceRequestById(@PathVariable Long id) {
        MaintenanceRequestResponse response = maintenanceService.getMaintenanceRequestById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/requests/number/{requestNumber}")
    @Operation(summary = "Get maintenance request by number", description = "Retrieve maintenance request by request number")
    public ResponseEntity<MaintenanceRequestResponse> getMaintenanceRequestByNumber(@PathVariable String requestNumber) {
        MaintenanceRequestResponse response = maintenanceService.getMaintenanceRequestByNumber(requestNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/requests/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get customer maintenance requests", description = "Get maintenance requests for authenticated customer")
    public ResponseEntity<Page<MaintenanceRequestResponse>> getMyMaintenanceRequests(
            @AuthenticationPrincipal UserPrincipal userPrincipal, Pageable pageable) {
        Page<MaintenanceRequestResponse> requests = maintenanceService.getCustomerMaintenanceRequests(userPrincipal.getId(), pageable);
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/requests")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get all maintenance requests", description = "Get all maintenance requests (Admin only)")
    public ResponseEntity<Page<MaintenanceRequestResponse>> getAllMaintenanceRequests(Pageable pageable) {
        Page<MaintenanceRequestResponse> requests = maintenanceService.getAllMaintenanceRequests(pageable);
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/requests/pending")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get pending requests", description = "Get pending maintenance requests (Admin only)")
    public ResponseEntity<List<MaintenanceRequestResponse>> getPendingRequests() {
        List<MaintenanceRequestResponse> requests = maintenanceService.getPendingRequests();
        return ResponseEntity.ok(requests);
    }

    @PostMapping("/requests/{id}/approve")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Approve maintenance request", description = "Approve maintenance request (Admin only)")
    public ResponseEntity<MaintenanceRequestResponse> approveMaintenanceRequest(
            @PathVariable Long id,
            @RequestParam BigDecimal estimatedCost,
            @RequestParam String adminNotes,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime estimatedCompletionDate) {
        MaintenanceRequestResponse response = maintenanceService.approveMaintenanceRequest(id, estimatedCost, adminNotes, estimatedCompletionDate);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/requests/{id}/reject")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Reject maintenance request", description = "Reject maintenance request (Admin only)")
    public ResponseEntity<MaintenanceRequestResponse> rejectMaintenanceRequest(
            @PathVariable Long id,
            @RequestParam String reason) {
        MaintenanceRequestResponse response = maintenanceService.rejectMaintenanceRequest(id, reason);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/requests/{id}/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update maintenance status", description = "Update maintenance request status (Admin only)")
    public ResponseEntity<MaintenanceRequestResponse> updateMaintenanceStatus(
            @PathVariable Long id,
            @RequestParam MaintenanceStatus status) {
        MaintenanceRequestResponse response = maintenanceService.updateMaintenanceStatus(id, status);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/requests/{id}/generate-ticket")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Generate maintenance ticket", description = "Generate maintenance ticket PDF (Admin only)")
    public ResponseEntity<MaintenanceTicketResponse> generateMaintenanceTicket(@PathVariable Long id) {
        MaintenanceTicketResponse response = maintenanceService.generateMaintenanceTicket(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tickets/{ticketNumber}")
    @Operation(summary = "Get maintenance ticket", description = "Get maintenance ticket by ticket number")
    public ResponseEntity<MaintenanceTicketResponse> getMaintenanceTicketByNumber(@PathVariable String ticketNumber) {
        MaintenanceTicketResponse response = maintenanceService.getMaintenanceTicketByNumber(ticketNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/requests/overdue")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get overdue requests", description = "Get overdue maintenance requests (Admin only)")
    public ResponseEntity<List<MaintenanceRequestResponse>> getOverdueRequests() {
        List<MaintenanceRequestResponse> requests = maintenanceService.getOverdueRequests();
        return ResponseEntity.ok(requests);
    }
}
