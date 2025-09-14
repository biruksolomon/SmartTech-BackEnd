package com.smarttech.mapper;

import com.smarttech.dto.response.MaintenanceRequestResponse;
import com.smarttech.dto.response.MaintenanceTicketResponse;
import com.smarttech.entity.MaintenanceRequest;
import com.smarttech.entity.MaintenanceTicket;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {UserMapper.class})
public interface MaintenanceMapper {
    MaintenanceRequestResponse toResponse(MaintenanceRequest maintenanceRequest);
    MaintenanceTicketResponse toTicketResponse(MaintenanceTicket maintenanceTicket);
}
