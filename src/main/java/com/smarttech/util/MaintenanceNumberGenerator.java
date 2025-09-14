package com.smarttech.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public class MaintenanceNumberGenerator {
    
    private static final String PREFIX = "MR";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    public static String generateRequestNumber() {
        String datePart = LocalDateTime.now().format(DATE_FORMAT);
        int randomPart = ThreadLocalRandom.current().nextInt(100, 999);
        return PREFIX + datePart + randomPart;
    }
    
    public static String generateTicketNumber() {
        return String.format("%08d", ThreadLocalRandom.current().nextInt(10000000, 99999999));
    }
}
