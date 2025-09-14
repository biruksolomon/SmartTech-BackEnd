package com.smarttech.service;

import com.smarttech.dto.request.PaymentRequestDTO;
//import com.smarttech.dto.SubAccountRequestDTO;
import com.yaphet.chapa.model.InitializeResponseData;

import java.math.BigDecimal;

public interface ChapaService {
    InitializeResponseData initializePayment(PaymentRequestDTO req);
//    SubAccountResponseData createSubAccount(SubAccountRequestDTO req);
//    boolean processSellerPayout(Long selectedAccountId, BigDecimal amount, String payoutReference);
    boolean verifyWebhookSignature(String payload, String signature);
    void processWebhookPayment(String payload);
    void processWebhookTransfer(String payload);
}
