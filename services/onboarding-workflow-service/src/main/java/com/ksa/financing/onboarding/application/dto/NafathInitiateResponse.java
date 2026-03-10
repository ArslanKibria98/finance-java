package com.ksa.financing.onboarding.application.dto;

import java.util.List;

public record NafathInitiateResponse(
    String workflowId,
    String status,
    String currentStep,
    String nextAction,
    String globalUid,
    String customerId,
    int nafathRandomNumber,
    String nafathSessionId,
    String transactionId,
    List<StepInfo> steps,
    String timestamp
) {}
