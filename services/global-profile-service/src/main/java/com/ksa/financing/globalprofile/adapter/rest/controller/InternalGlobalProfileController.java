package com.ksa.financing.globalprofile.adapter.rest.controller;

import com.ksa.financing.globalprofile.domain.model.GlobalCustomer;
import com.ksa.financing.globalprofile.domain.port.in.CreateGlobalProfileUseCase;
import com.ksa.financing.globalprofile.domain.port.out.GlobalCustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Internal service-to-service endpoint for creating a global customer profile
 * without JWT. Used by onboarding-workflow-service to register a freshly
 * onboarded user in the zero-PII customer 360 index.
 *
 * <p>Path is exempted from auth via SecurityConfig {@code /internal/**} permitAll.</p>
 */
@RestController
@RequestMapping("/internal/profiles")
@RequiredArgsConstructor
@Slf4j
public class InternalGlobalProfileController {

    private final CreateGlobalProfileUseCase createGlobalProfileUseCase;
    private final GlobalCustomerRepository globalCustomerRepository;

    @PostMapping
    public ResponseEntity<CreateGlobalProfileInternalResponse> createInternal(
            @RequestBody CreateGlobalProfileInternalRequest request) {
        log.info("Internal: Creating global profile for country={}", request.primaryCountryCode());

        GlobalCustomer created = createGlobalProfileUseCase.create(
                new CreateGlobalProfileUseCase.CreateGlobalProfileCommand(
                        request.email(),
                        request.mobile(),
                        request.primaryCountryCode()
                ));

        return ResponseEntity.status(HttpStatus.CREATED).body(new CreateGlobalProfileInternalResponse(
                created.getGlobalUid(),
                created.getGlobalEmailHash(),
                created.getGlobalMobileHash(),
                created.getPrimaryCountryCode()
        ));
    }

    public record CreateGlobalProfileInternalRequest(
            String email,
            String mobile,
            String primaryCountryCode
    ) {}

    public record CreateGlobalProfileInternalResponse(
            UUID globalUid,
            String emailHash,
            String mobileHash,
            String primaryCountryCode
    ) {}

    /**
     * Upfront duplicate check used by onboarding-workflow-service at {@code /initiate}.
     * Returns whether the given email and/or mobile is already associated with a
     * {@code global_customers} row.
     */
    @GetMapping("/exists")
    public ResponseEntity<ExistsResponse> exists(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String mobile) {
        boolean emailExists = false;
        boolean mobileExists = false;
        if (email != null && !email.isBlank()) {
            String emailHash = sha256Hash(email.toLowerCase());
            emailExists = globalCustomerRepository.findByEmailHash(emailHash).isPresent();
        }
        if (mobile != null && !mobile.isBlank()) {
            String mobileHash = sha256Hash(mobile);
            mobileExists = globalCustomerRepository.findByMobileHash(mobileHash).isPresent();
        }
        return ResponseEntity.ok(new ExistsResponse(emailExists, mobileExists));
    }

    public record ExistsResponse(boolean emailExists, boolean mobileExists) {}

    private static String sha256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) sb.append('0');
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
