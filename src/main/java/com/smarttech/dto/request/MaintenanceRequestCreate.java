package com.smarttech.dto.request;

import com.smarttech.enums.MaintenanceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class MaintenanceRequestCreate {
    @NotBlank(message = "Device type is required")
    private String deviceType;

    private String deviceModel;
    private String serialNumber;

    @NotBlank(message = "Issue description is required")
    private String issueDescription;

    @NotNull(message = "Maintenance type is required")
    private MaintenanceType maintenanceType;

    private List<String> imageUrls;
}
