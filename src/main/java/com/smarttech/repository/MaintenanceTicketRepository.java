package com.smarttech.repository;

import com.smarttech.entity.MaintenanceTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MaintenanceTicketRepository extends JpaRepository<MaintenanceTicket, Long> {
    Optional<MaintenanceTicket> findByTicketNumber(String ticketNumber);
    Optional<MaintenanceTicket> findByMaintenanceRequestId(Long maintenanceRequestId);
}
