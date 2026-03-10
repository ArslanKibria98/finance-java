package com.ksa.financing.customer.workflow.activity.impl;

import com.ksa.financing.customer.domain.model.Customer;
import com.ksa.financing.customer.domain.port.in.CreateCustomerUseCase;
import com.ksa.islamic.orchestration.activity.customer.ProfileCreationActivity;
import io.temporal.activity.Activity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class ProfileCreationActivityImpl implements ProfileCreationActivity {

    private final CreateCustomerUseCase createCustomerUseCase;

    @Override
    public ProfileCreationResult createProfile(ProfileCreationInput input) {
        log.info("Creating customer profile for nationalId={}", input.nationalId());
        try {
            String[] nameParts = splitFullName(input.fullNameEn());
            String[] namePartsAr = splitFullName(input.fullNameAr());

            UUID tenantUuid;
            try {
                tenantUuid = UUID.fromString(input.tenantId());
            } catch (IllegalArgumentException e) {
                tenantUuid = UUID.nameUUIDFromBytes(input.tenantId().getBytes());
            }

            UUID preAssignedGlobalUid = null;
            if (input.globalUid() != null && !input.globalUid().isBlank()) {
                try {
                    preAssignedGlobalUid = UUID.fromString(input.globalUid());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid globalUid from workflow (will create new via GPS): {}", input.globalUid());
                }
            }

            Customer customer = createCustomerUseCase.create(
                    new CreateCustomerUseCase.CreateCustomerCommand(
                            tenantUuid,
                            input.nationalId(),
                            "NID",
                            nameParts[0],
                            nameParts.length > 2 ? nameParts[1] : null,
                            nameParts[nameParts.length - 1],
                            namePartsAr[0],
                            namePartsAr[namePartsAr.length - 1],
                            input.dateOfBirth() != null ? LocalDate.parse(input.dateOfBirth()) : null,
                            input.gender(),
                            input.nationality(),
                            "CITIZEN",
                            input.mobileNumber(),
                            input.email(),
                            null,
                            input.lifecycleStage() != null ? input.lifecycleStage() : "ONBOARDING",
                            preAssignedGlobalUid,
                            null  // idempotencyKey — Temporal provides its own idempotency via workflow ID
                    )
            );

            return new ProfileCreationResult(
                    customer.getId().toString(),
                    customer.getGlobalUid() != null ? customer.getGlobalUid().toString() : null,
                    customer.getCifNumber(),
                    true
            );
        } catch (Exception e) {
            log.error("Profile creation failed for nationalId={}: {}", input.nationalId(), e.getMessage(), e);
            throw Activity.wrap(e);
        }
    }

    private String[] splitFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return new String[]{"", ""};
        }
        return fullName.trim().split("\\s+");
    }
}
