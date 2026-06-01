package com.ksa.financing.piivault.adapter.rest.controller;

import com.ksa.financing.piivault.domain.model.PiiIndividual;
import com.ksa.financing.piivault.domain.port.in.StorePiiUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Internal service-to-service endpoint for storing PII without JWT.
 * Used by onboarding-workflow-service to persist a customer's encrypted PII
 * immediately after Facia OCR + user confirmation.
 *
 * <p>Trust boundary: only callable on the internal Docker network. Path is
 * exempted from auth via SecurityConfig {@code /internal/**} permitAll.</p>
 */
@RestController
@RequestMapping("/internal/pii")
@RequiredArgsConstructor
@Slf4j
public class InternalPiiVaultController {

    private final StorePiiUseCase storePiiUseCase;

    @PostMapping("/individual")
    public ResponseEntity<StoreInternalPiiResponse> storeInternal(
            @RequestBody StoreInternalPiiRequest request) {
        log.info("Internal: Storing PII for globalUid={} country={}",
                request.globalUid(), request.countryCode());

        PiiIndividual stored = storePiiUseCase.store(new StorePiiUseCase.StorePiiCommand(
                request.globalUid(),
                request.nationalId(),
                request.nationalIdType(),
                request.fullName(),
                request.firstName(),
                request.middleName(),
                request.lastName(),
                request.fullNameAr(),
                request.dateOfBirth(),
                request.gender(),
                request.nationalityCode(),
                request.mobile(),
                request.email(),
                request.countryCode()
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(new StoreInternalPiiResponse(
                stored.getGlobalUid(),
                "STORED"
        ));
    }

    public record StoreInternalPiiRequest(
            UUID globalUid,
            String nationalId,
            String nationalIdType,
            String fullName,
            String firstName,
            String middleName,
            String lastName,
            String fullNameAr,
            String dateOfBirth,
            String gender,
            String nationalityCode,
            String mobile,
            String email,
            String countryCode
    ) {}

    public record StoreInternalPiiResponse(
            UUID globalUid,
            String status
    ) {}
}
