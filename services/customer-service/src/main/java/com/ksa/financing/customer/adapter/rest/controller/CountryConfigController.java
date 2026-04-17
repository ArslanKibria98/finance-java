package com.ksa.financing.customer.adapter.rest.controller;

import com.ksa.financing.customer.domain.model.CountryOnboardingProfile;
import com.ksa.financing.customer.domain.model.SupportedCountry;
import com.ksa.financing.customer.domain.port.out.CountryConfigRepository;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/country-config")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Country Configuration", description = "Country-specific onboarding configuration endpoints")
public class CountryConfigController {

    private final CountryConfigRepository countryConfigRepository;

    @GetMapping("/countries")
    @Operation(summary = "List supported countries", description = "Returns all active countries with their KYC configuration")
    public ResponseEntity<List<CountryResponse>> listCountries() {
        log.info("Listing supported countries");
        var countries = countryConfigRepository.findAllActiveCountries().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(countries);
    }

    @GetMapping("/{countryCode}")
    @Operation(summary = "Get country details", description = "Returns country configuration with supported ID types and KYC providers")
    public ResponseEntity<CountryResponse> getCountry(
            @PathVariable String countryCode) {
        log.info("Getting country config for: {}", countryCode);
        var country = countryConfigRepository.findCountryByCode(countryCode.toUpperCase())
                .orElseThrow(() -> new BusinessException(
                        ErrorCodes.NOT_FOUND,
                        "Country not found: " + countryCode));
        return ResponseEntity.ok(toResponse(country));
    }

    @GetMapping("/{countryCode}/steps")
    @Operation(summary = "Get onboarding steps for a country", description = "Returns the ordered onboarding step configuration for a specific country")
    public ResponseEntity<CountryStepsResponse> getCountrySteps(
            @PathVariable String countryCode) {
        log.info("Getting onboarding steps for country: {}", countryCode);

        String code = countryCode.toUpperCase();
        if (!countryConfigRepository.isCountrySupported(code)) {
            throw new BusinessException(
                    ErrorCodes.NOT_FOUND,
                    "Country not supported: " + countryCode);
        }

        var steps = countryConfigRepository.findStepsByCountryCode(code).stream()
                .map(this::toStepResponse)
                .toList();

        var country = countryConfigRepository.findCountryByCode(code).orElse(null);

        return ResponseEntity.ok(new CountryStepsResponse(
                code,
                country != null ? country.countryName() : code,
                country != null ? country.countryNameAr() : null,
                steps.size(),
                steps
        ));
    }

    // ---- Response records ----

    public record CountryResponse(
            String countryCode,
            String countryName,
            String countryNameAr,
            String currencyCode,
            String flagEmoji,
            String dialCode,
            String nationalityEn,
            String nationalityAr,
            boolean active,
            List<String> idTypes,
            String defaultIdType,
            List<String> kycProviders
    ) {}

    public record CountryStepsResponse(
            String countryCode,
            String countryName,
            String countryNameAr,
            int totalSteps,
            List<StepResponse> steps
    ) {}

    public record StepResponse(
            int stepOrder,
            String stepType,
            String stepLabel,
            String stepLabelAr,
            String description,
            String providerCode,
            boolean signalWait,
            int timeoutMinutes,
            boolean required
    ) {}

    private CountryResponse toResponse(SupportedCountry c) {
        return new CountryResponse(
                c.countryCode(),
                c.countryName(),
                c.countryNameAr(),
                c.currencyCode(),
                c.flagEmoji(),
                c.dialCode(),
                c.nationalityEn(),
                c.nationalityAr(),
                c.active(),
                List.of(c.idTypes().split(",")),
                c.defaultIdType(),
                List.of(c.kycProviders().split(","))
        );
    }

    private StepResponse toStepResponse(CountryOnboardingProfile p) {
        return new StepResponse(
                p.getStepOrder(),
                p.getStepType(),
                p.getStepLabel(),
                p.getStepLabelAr(),
                p.getDescription(),
                p.getProviderCode(),
                p.isSignalWait(),
                p.getTimeoutMinutes(),
                p.isRequired()
        );
    }
}
