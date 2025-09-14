package com.smarttech.service;

import java.util.List;

public interface SmsService {
    void sendOtp(String phoneNumber);
    void sendOtp(String phoneNumber, String otp);
    Boolean verifyOtp(String phoneNumber, String otp);
    void sendSellerWelcomeSms(String phoneNumber, String password);
    void sendSms(String phoneNumber, String message);
    void sendMessage(String phoneNumber, String message);
    void sendBulkSms(List<String> phoneNumbers, String message, String campaignName);
}
