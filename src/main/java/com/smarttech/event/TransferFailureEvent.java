package com.smarttech.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TransferFailureEvent {
    private String transferReference;
    private String reason;
}
