package com.ksa.financing.wallet.adapter.rest.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.wallet.adapter.rest.support.AuthWalletResolver;
import com.ksa.financing.wallet.application.dto.IbftBeneficiaryDtos.AddIbftBeneficiaryRequest;
import com.ksa.financing.wallet.application.dto.IbftBeneficiaryDtos.IbftBeneficiaryResponse;
import com.ksa.financing.wallet.application.dto.IbftBeneficiaryDtos.ValidateAccountRequest;
import com.ksa.financing.wallet.application.dto.IbftBeneficiaryDtos.ValidateAccountResponse;
import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.port.in.ManageIbftBeneficiaryUseCase;
import com.ksa.financing.wallet.domain.port.in.ValidateAccountUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/ibft/beneficiaries")
@RequiredArgsConstructor
@Tag(name = "IBFT Beneficiaries", description = "External Canadian bank-account beneficiaries for IBFT")
public class IbftBeneficiaryController {

    private final ManageIbftBeneficiaryUseCase useCase;
    private final ValidateAccountUseCase validateAccountUseCase;
    private final AuthWalletResolver auth;
    private final ObjectMapper objectMapper;

    @SecuredEndpoint(obj = "ibft.beneficiaries", act = "create")
    @PostMapping
    @Operation(summary = "Add an IBFT beneficiary (validated via Scotia account-validation)")
    public ResponseEntity<IbftBeneficiaryResponse> add(
            @Valid @RequestBody AddIbftBeneficiaryRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Wallet w = auth.wallet(jwt);
        var b = useCase.add(new ManageIbftBeneficiaryUseCase.AddBeneficiaryCommand(
                w.getTenantId(), w.getCustomerId(), w.getId(),
                request.nickname(), request.beneficiaryName(),
                request.institutionNumber(), request.accountNumber(),
                request.bankName(), request.currency()));
        return ResponseEntity.status(HttpStatus.CREATED).body(IbftBeneficiaryResponse.from(b));
    }

    @SecuredEndpoint(obj = "ibft.beneficiaries", act = "create")
    @PostMapping("/validate-account")
    @Operation(summary = "Validate a destination account — must exist in our wallets, then Scotia account-validation",
            description = "transit is derived from the first 5 digits of accountNumber (non-digits ignored). " +
                    "fullName is optional. If the account number is not present in our wallets, returns " +
                    "valid=false / status=NO_MATCH_FOUND without calling Scotia.")
    public ResponseEntity<ValidateAccountResponse> validateAccount(
            @Valid @RequestBody ValidateAccountRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = auth.tenantId(jwt);
        var r = validateAccountUseCase.validate(tenantId, request.accountNumber(),
                request.institutionNumber(), request.fullName(), request.currency());
        return ResponseEntity.ok(new ValidateAccountResponse(
                r.valid(), r.status(), r.accountNumber(), r.transit(), r.institutionNumber(),
                r.message(), r.scotiaRef(), parseJson(r.scotiaRaw())));
    }

    /** Parse the raw Scotia JSON so it embeds as a nested object (not an escaped string); null-safe. */
    private Object parseJson(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            return raw;
        }
    }

    @SecuredEndpoint(obj = "ibft.beneficiaries", act = "list")
    @GetMapping
    @Operation(summary = "List my IBFT beneficiaries")
    public List<IbftBeneficiaryResponse> list(@AuthenticationPrincipal Jwt jwt) {
        Wallet w = auth.wallet(jwt);
        return useCase.list(w.getTenantId(), w.getCustomerId()).stream()
                .map(IbftBeneficiaryResponse::from).toList();
    }

    @SecuredEndpoint(obj = "ibft.beneficiaries", act = "read")
    @GetMapping("/{id}")
    @Operation(summary = "Get an IBFT beneficiary")
    public ResponseEntity<IbftBeneficiaryResponse> get(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(IbftBeneficiaryResponse.from(useCase.get(auth.tenantId(jwt), id)));
    }

    @SecuredEndpoint(obj = "ibft.beneficiaries", act = "delete")
    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate an IBFT beneficiary")
    public ResponseEntity<IbftBeneficiaryResponse> deactivate(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(IbftBeneficiaryResponse.from(useCase.deactivate(auth.tenantId(jwt), id)));
    }
}
