package com.smarttech.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TransferSuccessEvent {
    private String transferReference;
    private String webhookData;
}
