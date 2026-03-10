package com.ksa.financing.onboarding.application.dto;

import java.util.List;
import java.util.Map;

public record OnboardingStatusResponse(
    String workflowId,
    String currentStep,
    String status,
    String nextAction,
    String customerId,
    String globalUid,
    String walletId,
    String keycloakUserId,
    String otpRequestId,
    int nafathRandomNumber,
    boolean deviceTrusted,
    String lifecycleStage,
    String failureReason,
    Map<String, Object> nafathVerificationData,
    Map<String, Object> yakeenData,
    List<StepInfo> steps,
    String startedAt,
    String lastUpdatedAt
) {}
