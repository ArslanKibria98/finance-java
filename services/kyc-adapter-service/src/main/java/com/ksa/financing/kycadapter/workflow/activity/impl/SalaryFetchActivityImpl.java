package com.ksa.financing.kycadapter.workflow.activity.impl;

import com.ksa.financing.kycadapter.domain.port.in.FetchSalaryUseCase;
import com.ksa.islamic.orchestration.activity.kyc.SalaryFetchActivity;
import io.temporal.activity.Activity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class SalaryFetchActivityImpl implements SalaryFetchActivity {

    private final FetchSalaryUseCase fetchSalaryUseCase;

    @Override
    public SalaryFetchResult fetchSalary(SalaryFetchInput input) {
        log.info("Fetching salary for nationalId={}", input.nationalId());
        try {
            UUID tenantUuid;
            try {
                tenantUuid = UUID.fromString(input.tenantId());
            } catch (IllegalArgumentException e) {
                tenantUuid = UUID.nameUUIDFromBytes(input.tenantId().getBytes());
            }

            FetchSalaryUseCase.SalaryResult result = fetchSalaryUseCase.fetch(
                    new FetchSalaryUseCase.FetchSalaryCommand(
                            tenantUuid,
                            input.nationalId(),
                            Activity.getExecutionContext().getInfo().getActivityId()
                    )
            );

            return new SalaryFetchResult(
                    true,
                    result.employerName(),
                    result.basicSalary() != null ? result.basicSalary().doubleValue() : 0.0,
                    result.totalSalary() != null ? result.totalSalary().doubleValue() : 0.0,
                    result.totalSalary() != null ? result.totalSalary().doubleValue() : 0.0,
                    "SAR"
            );
        } catch (Exception e) {
            log.error("Salary fetch failed for nationalId={}: {}", input.nationalId(), e.getMessage(), e);
            throw Activity.wrap(e);
        }
    }
}
