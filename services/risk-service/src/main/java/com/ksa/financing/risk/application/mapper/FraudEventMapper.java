package com.ksa.financing.risk.application.mapper;

import com.ksa.financing.risk.application.dto.fraud.FraudEvaluationResponseDto;
import com.ksa.financing.risk.application.dto.fraud.FraudEventRequestDto;
import com.ksa.financing.risk.application.dto.fraud.FraudRuleConfigDto;
import com.ksa.financing.risk.domain.model.device.DeviceInfo;
import com.ksa.financing.risk.domain.model.device.DeviceIntegrityStatus;
import com.ksa.financing.risk.domain.model.device.DeviceOS;
import com.ksa.financing.risk.domain.model.device.DeviceType;
import com.ksa.financing.risk.domain.model.fraud.FraudEvaluationResult;
import com.ksa.financing.risk.domain.model.fraud.FraudEvent;
import com.ksa.financing.risk.domain.model.fraud.FraudEventType;
import com.ksa.financing.risk.domain.model.location.LocationData;
import com.ksa.financing.risk.domain.model.rule.FraudRule;
import com.ksa.financing.risk.domain.model.transaction.PaymentSource;

import java.time.LocalDateTime;
import java.util.UUID;

public final class FraudEventMapper {

    private FraudEventMapper() {}

    public static FraudEvent toDomain(UUID tenantId, FraudEventRequestDto dto) {
        var deviceInfo = new DeviceInfo(
                dto.deviceId(),
                parseEnum(DeviceType.class, dto.deviceType()),
                parseEnum(DeviceOS.class, dto.deviceOs()),
                dto.osVersion(),
                dto.deviceFingerprint(),
                parseEnum(DeviceIntegrityStatus.class, dto.deviceIntegrity())
        );

        var locationData = new LocationData(
                dto.latitude(), dto.longitude(),
                dto.ipAddress(), dto.ipCountry(), dto.ipCity(),
                dto.gpsCountry(), dto.gpsCity(),
                Boolean.TRUE.equals(dto.vpnDetected()),
                Boolean.TRUE.equals(dto.proxyDetected())
        );

        var paymentSource = (dto.paymentIban() != null || dto.cardLast4() != null)
                ? new PaymentSource(dto.paymentIban(), dto.cardLast4(),
                    dto.cardCountry(), dto.cardHolderName(),
                    Boolean.TRUE.equals(dto.thirdPartyPayment()))
                : null;

        return new FraudEvent(
                null, tenantId, dto.eventId(),
                parseEnum(FraudEventType.class, dto.eventType()),
                dto.customerId(), dto.nationalIdHash(),
                deviceInfo, locationData,
                dto.eventTimestamp(), dto.sessionId(),
                dto.transactionType(), dto.transactionAmount(), dto.currency(),
                dto.loanApplicationId(), dto.loanProductType(), dto.approvedLoanAmount(),
                dto.disbursementIban(), dto.ibanVerificationStatus(), dto.ibanHolderName(),
                paymentSource,
                null, null, null, null, false, false,
                LocalDateTime.now(), dto.correlationId()
        );
    }

    public static FraudEvaluationResponseDto toResponseDto(FraudEvaluationResult result) {
        var triggeredRules = result.triggeredRules().stream()
                .map(r -> new FraudEvaluationResponseDto.TriggeredRuleDto(
                        r.ruleId().name(),
                        r.decision() != null ? r.decision().name() : null,
                        r.blockType() != null ? r.blockType().name() : null,
                        r.detail(),
                        r.scoreContribution()
                ))
                .toList();

        return new FraudEvaluationResponseDto(
                result.id(),
                result.eventId(),
                result.customerId(),
                result.decision().name(),
                result.blockType() != null ? result.blockType().name() : null,
                result.compositeRiskScore(),
                result.riskLevel(),
                triggeredRules,
                result.blockReason(),
                result.blockDurationHours(),
                result.customerMessage(),
                result.evaluationTimeMs(),
                result.evaluatedAt()
        );
    }

    public static FraudRuleConfigDto toRuleConfigDto(FraudRule rule) {
        var params = rule.parameters() != null
                ? rule.parameters().stream()
                    .map(p -> new FraudRuleConfigDto.RuleParameterDto(
                            p.key(), p.value(), p.dataType(), p.description(), p.descriptionAr()))
                    .toList()
                : java.util.List.<FraudRuleConfigDto.RuleParameterDto>of();

        return new FraudRuleConfigDto(
                rule.id(),
                rule.ruleId().name(),
                rule.scenarioName(), rule.scenarioNameAr(),
                rule.category().name(),
                rule.detectionLogic(),
                rule.defaultAction().name(),
                rule.blockType() != null ? rule.blockType().name() : null,
                rule.status().name(),
                params,
                rule.priority()
        );
    }

    private static <T extends Enum<T>> T parseEnum(Class<T> enumClass, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
