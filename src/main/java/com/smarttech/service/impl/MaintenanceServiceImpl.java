package com.smarttech.service.impl;

import com.smarttech.dto.request.MaintenanceRequestCreate;
import com.smarttech.dto.response.MaintenanceRequestResponse;
import com.smarttech.dto.response.MaintenanceTicketResponse;
import com.smarttech.entity.MaintenanceRequest;
import com.smarttech.entity.MaintenanceTicket;
import com.smarttech.entity.User;
import com.smarttech.enums.MaintenanceStatus;
import com.smarttech.exception.ResourceNotFoundException;
import com.smarttech.mapper.MaintenanceMapper;
import com.smarttech.repository.MaintenanceRequestRepository;
import com.smarttech.repository.MaintenanceTicketRepository;
import com.smarttech.service.MaintenanceService;
import com.smarttech.service.PdfGenerationService;
import com.smarttech.service.UserService;
import com.smarttech.util.MaintenanceNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MaintenanceServiceImpl implements MaintenanceService {

    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final MaintenanceTicketRepository maintenanceTicketRepository;
    private final MaintenanceMapper maintenanceMapper;
    private final UserService userService;
    private final PdfGenerationService pdfGenerationService;

    @Override
    public MaintenanceRequestResponse createMaintenanceRequest(MaintenanceRequestCreate request, Long customerId) {
        log.info("Creating maintenance request for customer ID: {}", customerId);

        User customer = userService.findEntityById(customerId);

        MaintenanceRequest maintenanceRequest = MaintenanceRequest.builder()
                .requestNumber(MaintenanceNumberGenerator.generateRequestNumber())
                .customer(customer)
                .deviceType(request.getDeviceType())
                .deviceModel(request.getDeviceModel())
                .serialNumber(request.getSerialNumber())
                .issueDescription(request.getIssueDescription())
                .maintenanceType(request.getMaintenanceType())
                .status(MaintenanceStatus.PENDING)
                .imageUrls(request.getImageUrls())
                .isWarrantyCovered(false) // Will be determined by admin
                .build();

        MaintenanceRequest savedRequest = maintenanceRequestRepository.save(maintenanceRequest);
        log.info("Maintenance request created with number: {}", savedRequest.getRequestNumber());

        return maintenanceMapper.toResponse(savedRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public MaintenanceRequestResponse getMaintenanceRequestById(Long id) {
        MaintenanceRequest request = findEntityById(id);
        return maintenanceMapper.toResponse(request);
    }

    @Override
    @Transactional(readOnly = true)
    public MaintenanceRequestResponse getMaintenanceRequestByNumber(String requestNumber) {
        MaintenanceRequest request = maintenanceRequestRepository.findByRequestNumber(requestNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance request not found with number: " + requestNumber));
        return maintenanceMapper.toResponse(request);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MaintenanceRequestResponse> getCustomerMaintenanceRequests(Long customerId, Pageable pageable) {
        Page<MaintenanceRequest> requests = maintenanceRequestRepository.findByCustomerId(customerId, pageable);
        return requests.map(maintenanceMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MaintenanceRequestResponse> getAllMaintenanceRequests(Pageable pageable) {
        Page<MaintenanceRequest> requests = maintenanceRequestRepository.findAll(pageable);
        return requests.map(maintenanceMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceRequestResponse> getPendingRequests() {
        List<MaintenanceRequest> requests = maintenanceRequestRepository.findPendingRequestsOrderByDate();
        return requests.stream()
                .map(maintenanceMapper::toResponse)
                .toList();
    }

    @Override
    public MaintenanceRequestResponse approveMaintenanceRequest(Long requestId, BigDecimal estimatedCost,
                                                                String adminNotes, LocalDateTime estimatedCompletionDate) {
        log.info("Approving maintenance request ID: {}", requestId);

        MaintenanceRequest request = findEntityById(requestId);

        // Check warranty coverage based on serial number and purchase date
        boolean isWarrantyCovered = checkWarrantyCoverage(request.getSerialNumber());

        request.setStatus(MaintenanceStatus.APPROVED);
        request.setEstimatedCost(isWarrantyCovered ? BigDecimal.ZERO : estimatedCost);
        request.setAdminNotes(adminNotes);
        request.setEstimatedCompletionDate(estimatedCompletionDate);
        request.setIsWarrantyCovered(isWarrantyCovered);

        MaintenanceRequest savedRequest = maintenanceRequestRepository.save(request);
        log.info("Maintenance request approved: {}", request.getRequestNumber());

        return maintenanceMapper.toResponse(savedRequest);
    }

    @Override
    public MaintenanceRequestResponse rejectMaintenanceRequest(Long requestId, String reason) {
        log.info("Rejecting maintenance request ID: {}", requestId);

        MaintenanceRequest request = findEntityById(requestId);
        request.setStatus(MaintenanceStatus.REJECTED);
        request.setAdminNotes(reason);

        MaintenanceRequest savedRequest = maintenanceRequestRepository.save(request);
        log.info("Maintenance request rejected: {}", request.getRequestNumber());

        return maintenanceMapper.toResponse(savedRequest);
    }

    @Override
    public MaintenanceRequestResponse updateMaintenanceStatus(Long requestId, MaintenanceStatus status) {
        log.info("Updating maintenance request status for ID: {} to {}", requestId, status);

        MaintenanceRequest request = findEntityById(requestId);
        request.setStatus(status);

        if (status == MaintenanceStatus.COMPLETED) {
            request.setCompletedDate(LocalDateTime.now());
        }

        MaintenanceRequest savedRequest = maintenanceRequestRepository.save(request);
        log.info("Maintenance request status updated: {}", request.getRequestNumber());

        return maintenanceMapper.toResponse(savedRequest);
    }

    @Override
    public MaintenanceTicketResponse generateMaintenanceTicket(Long requestId) {
        log.info("Generating maintenance ticket for request ID: {}", requestId);

        MaintenanceRequest request = findEntityById(requestId);

        if (request.getStatus() != MaintenanceStatus.APPROVED) {
            throw new IllegalStateException("Cannot generate ticket for non-approved request");
        }

        // Generate PDF
        String pdfUrl = pdfGenerationService.generateMaintenanceTicketPdf(request);

        // Create ticket record
        MaintenanceTicket ticket = pdfGenerationService.createMaintenanceTicketRecord(request, pdfUrl);

        log.info("Maintenance ticket generated: {}", ticket.getTicketNumber());
        return maintenanceMapper.toTicketResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public MaintenanceTicketResponse getMaintenanceTicketByNumber(String ticketNumber) {
        MaintenanceTicket ticket = maintenanceTicketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance ticket not found with number: " + ticketNumber));
        return maintenanceMapper.toTicketResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceRequestResponse> getOverdueRequests() {
        List<MaintenanceRequest> requests = maintenanceRequestRepository.findOverdueRequests(LocalDateTime.now());
        return requests.stream()
                .map(maintenanceMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MaintenanceRequest findEntityById(Long id) {
        return maintenanceRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance request not found with ID: " + id));
    }

    public String getMaintenanceTicketPdfUrl(String ticketNumber) {
        return maintenanceTicketRepository.findByTicketNumber(ticketNumber)
                .map(MaintenanceTicket::getPdfUrl)
                .orElse(null);
    }

    private boolean checkWarrantyCoverage(String serialNumber) {
        // This would check against product warranty information
        // For now, return false as placeholder
        // In real implementation, you'd check:
        // 1. Find product by serial number
        // 2. Check purchase date
        // 3. Compare with warranty period
        return false;
    }
}
