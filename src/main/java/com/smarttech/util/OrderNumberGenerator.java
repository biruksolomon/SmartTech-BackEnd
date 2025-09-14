package com.smarttech.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public class OrderNumberGenerator {
    
    private static final String PREFIX = "ST";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    
    public static String generate() {
        String datePart = LocalDateTime.now().format(DATE_FORMAT);
        int randomPart = ThreadLocalRandom.current().nextInt(1000, 9999);
        return PREFIX + datePart + randomPart;
    }
}
