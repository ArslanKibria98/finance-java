package com.ksa.financing.customer.adapter.rest.controller;

import com.ksa.financing.customer.domain.model.Customer;
import com.ksa.financing.customer.domain.port.in.CreateCustomerUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Internal service-to-service endpoint used by onboarding-workflow-service to
 * persist a customer as a {@code LEAD} (lifecycle stage = LEAD) after the user
 * finishes the lightweight onboarding journey (Canada / Foreign / Guest).
 *
 * <p>The customer is NOT a fully-onboarded retail customer yet — they become one
 * when they apply for their first product (loan / wallet activation / etc.).
 * No JWT required — guarded by Spring SecurityConfig's {@code /internal/**} permitAll
 * rule and trusted only on the internal network.</p>
 */
@RestController
@RequestMapping("/internal/customers")
@RequiredArgsConstructor
@Slf4j
public class InternalCustomerOnboardingController {

    private final CreateCustomerUseCase createCustomerUseCase;

    @PostMapping("/lead")
    public ResponseEntity<CreateLeadCustomerResponse> createLeadCustomer(
            @Valid @RequestBody CreateLeadCustomerRequest req) {
        log.info("Creating LEAD customer: flow={} email={} keycloakUserId={}",
                req.flowType(), maskEmail(req.email()), req.keycloakUserId());

        // Synthesise a unique national_id when the flow doesn't have one (e.g. Guest).
        String nationalId = req.nationalId();
        String nationalIdType = req.nationalIdType();
        if (nationalId == null || nationalId.isBlank()) {
            nationalId = "LEAD-" + req.keycloakUserId();
            nationalIdType = nationalIdType != null ? nationalIdType : "LEAD_REF";
        }

        Customer created = createCustomerUseCase.create(new CreateCustomerUseCase.CreateCustomerCommand(
                req.tenantId(),
                nationalId,
                nationalIdType,
                req.firstName(),
                req.middleName(),
                req.lastName(),
                req.firstNameAr(),
                req.lastNameAr(),
                req.dateOfBirth(),
                req.gender(),
                req.nationality(),
                req.residencyType(),
                req.mobileNumber(),
                req.email(),
                req.keycloakUserId(),
                "LEAD",
                req.globalUid(),
                req.idempotencyKey(),
                req.flowType()
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(new CreateLeadCustomerResponse(
                created.getId(),
                created.getCifNumber(),
                "LEAD",
                req.globalUid(),
                req.flowType()
        ));
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "****";
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 2) return "**" + domain;
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }

    // -------------------------------------------------------------------------
    // DTOs
    // -------------------------------------------------------------------------

    public record CreateLeadCustomerRequest(
            @NotNull UUID tenantId,
            @NotNull UUID keycloakUserId,
            @NotBlank String flowType,             // CANADA | FOREIGN | GUEST
            @Email String email,
            String mobileNumber,
            UUID globalUid,
            String nationalId,                      // optional — synthesised if blank
            String nationalIdType,                  // PASSPORT | ID | LEAD_REF (default)
            String firstName,
            String middleName,
            String lastName,
            String firstNameAr,
            String lastNameAr,
            LocalDate dateOfBirth,
            String gender,
            String nationality,
            String residencyType,
            String idempotencyKey
    ) {}

    public record CreateLeadCustomerResponse(
            UUID customerId,
            String cifNumber,
            String lifecycleStage,
            UUID globalUid,
            String flowType
    ) {}
}
