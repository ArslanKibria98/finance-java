package com.ksa.financing.kycadapter.workflow.activity.impl;

import com.ksa.financing.kycadapter.domain.port.in.VerifyIdentityUseCase;
import com.ksa.islamic.orchestration.activity.kyc.YakeenVerificationActivity;
import io.temporal.activity.Activity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class YakeenVerificationActivityImpl implements YakeenVerificationActivity {

    private final VerifyIdentityUseCase verifyIdentityUseCase;

    @Override
    public YakeenVerificationResult verifyIdentity(YakeenVerificationInput input) {
        log.info("Verifying identity via Yakeen for nationalId={}", input.nationalId());
        try {
            UUID tenantUuid;
            try {
                tenantUuid = UUID.fromString(input.tenantId());
            } catch (IllegalArgumentException e) {
                tenantUuid = UUID.nameUUIDFromBytes(input.tenantId().getBytes());
            }

            LocalDate dob = input.dateOfBirth() != null ? LocalDate.parse(input.dateOfBirth()) : null;

            VerifyIdentityUseCase.VerifyIdentityResult result = verifyIdentityUseCase.verify(
                    new VerifyIdentityUseCase.VerifyIdentityCommand(
                            tenantUuid,
                            input.nationalId(),
                            dob,
                            Activity.getExecutionContext().getInfo().getActivityId()
                    )
            );

            Map<String, Object> demographics = result.demographics();

            return new YakeenVerificationResult(
                    result.session().getStatus() != null
                            && "COMPLETED".equalsIgnoreCase(result.session().getStatus().name()),
                    getStringValue(demographics, "fullNameAr"),
                    getStringValue(demographics, "fullNameEn"),
                    getStringValue(demographics, "gender"),
                    getStringValue(demographics, "nationality"),
                    getStringValue(demographics, "addressCity"),
                    getStringValue(demographics, "addressRegion")
            );
        } catch (Exception e) {
            log.error("Yakeen verification failed for nationalId={}: {}", input.nationalId(), e.getMessage(), e);
            throw Activity.wrap(e);
        }
    }

    private String getStringValue(Map<String, Object> map, String key) {
        if (map == null) return null;
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}
