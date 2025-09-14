package com.smarttech.repository;

import com.smarttech.entity.MaintenanceRequest;
import com.smarttech.enums.MaintenanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, Long> {
    Optional<MaintenanceRequest> findByRequestNumber(String requestNumber);
    Page<MaintenanceRequest> findByCustomerId(Long customerId, Pageable pageable);
    List<MaintenanceRequest> findByStatus(MaintenanceStatus status);
    Long countByStatus(MaintenanceStatus status);
    
    @Query("SELECT mr FROM MaintenanceRequest mr WHERE mr.serialNumber = :serialNumber")
    List<MaintenanceRequest> findBySerialNumber(@Param("serialNumber") String serialNumber);
    
    @Query("SELECT mr FROM MaintenanceRequest mr WHERE mr.status = 'PENDING' ORDER BY mr.createdAt ASC")
    List<MaintenanceRequest> findPendingRequestsOrderByDate();
    
    @Query("SELECT mr FROM MaintenanceRequest mr WHERE mr.estimatedCompletionDate < :currentDate AND mr.status = 'IN_PROGRESS'")
    List<MaintenanceRequest> findOverdueRequests(@Param("currentDate") LocalDateTime currentDate);
}
