package com.ksa.financing.wallet.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.wallet.application.dto.AddBeneficiaryRequest;
import com.ksa.financing.wallet.application.dto.BeneficiaryResponse;
import com.ksa.financing.wallet.domain.model.IbanBeneficiary;
import com.ksa.financing.wallet.domain.port.in.ManageBeneficiaryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/wallets/beneficiaries")
@RequiredArgsConstructor
@Tag(name = "IBAN Beneficiaries", description = "Saved IBAN beneficiaries for wallet withdrawals")
public class IbanBeneficiaryController {

    private final ManageBeneficiaryUseCase useCase;

    @SecuredEndpoint(obj = "wallet.beneficiaries", act = "create")
    @PostMapping
    @Operation(summary = "Add a new IBAN beneficiary for the authenticated customer")
    public ResponseEntity<BeneficiaryResponse> add(
            @Valid @RequestBody AddBeneficiaryRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        UUID customerId = extractCustomerId(jwt);
        IbanBeneficiary saved = useCase.add(new ManageBeneficiaryUseCase.AddBeneficiaryCommand(
                tenantId, customerId, request.walletId(),
                request.nickname(), request.beneficiaryName(),
                request.iban(), request.bankCode(), request.bankName()));
        return ResponseEntity.ok(toResponse(saved));
    }

    @SecuredEndpoint(obj = "wallet.beneficiaries", act = "list")
    @GetMapping
    @Operation(summary = "List active beneficiaries for the authenticated customer. Optional ?search= filters by nickname, beneficiaryName, iban, bankCode, bankName (case-insensitive LIKE).")
    public ResponseEntity<List<BeneficiaryResponse>> list(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String search,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        UUID customerId = extractCustomerId(jwt);
        String term = (search != null && !search.isBlank()) ? search.trim().toLowerCase() : null;
        List<BeneficiaryResponse> list = useCase.listByCustomer(tenantId, customerId)
                .stream()
                .map(this::toResponse)
                .filter(b -> term == null
                        || contains(b.nickname(), term)
                        || contains(b.beneficiaryName(), term)
                        || contains(b.iban(), term)
                        || contains(b.bankCode(), term)
                        || contains(b.bankName(), term))
                .toList();
        return ResponseEntity.ok(list);
    }

    private static boolean contains(String f, String term) {
        return f != null && f.toLowerCase().contains(term);
    }

    @SecuredEndpoint(obj = "wallet.beneficiaries", act = "read")
    @GetMapping("/{beneficiaryId}")
    @Operation(summary = "Get beneficiary by ID")
    public ResponseEntity<BeneficiaryResponse> getById(
            @PathVariable UUID beneficiaryId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(toResponse(useCase.getById(tenantId, beneficiaryId)));
    }

    @SecuredEndpoint(obj = "wallet.beneficiaries", act = "delete")
    @DeleteMapping("/{beneficiaryId}")
    @Operation(summary = "Deactivate a beneficiary")
    public ResponseEntity<Void> deactivate(
            @PathVariable UUID beneficiaryId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        useCase.deactivate(tenantId, beneficiaryId);
        return ResponseEntity.noContent().build();
    }

    @SecuredEndpoint(obj = "wallet.beneficiaries", act = "update")
    @PostMapping("/{beneficiaryId}/activate")
    @Operation(summary = "Re-activate a previously deactivated beneficiary")
    public ResponseEntity<BeneficiaryResponse> activate(
            @PathVariable UUID beneficiaryId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = extractTenantId(jwt);
        return ResponseEntity.ok(toResponse(useCase.activate(tenantId, beneficiaryId)));
    }

    private UUID extractTenantId(Jwt jwt) {
        String tenantClaim = jwt.getClaimAsString("tenant_id");
        if (tenantClaim == null) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantClaim);
    }

    private UUID extractCustomerId(Jwt jwt) {
        // customer_id claim if present, else fall back to sub
        String customer = jwt.getClaimAsString("customer_id");
        if (customer == null) customer = jwt.getSubject();
        if (customer == null) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "JWT missing customer_id and subject");
        }
        try {
            return UUID.fromString(customer);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    ErrorCodes.INVALID_CREDENTIALS,
                    "JWT customer_id/sub is not a valid UUID");
        }
    }

    private BeneficiaryResponse toResponse(IbanBeneficiary b) {
        return new BeneficiaryResponse(
                b.getId(),
                b.getCustomerId(),
                b.getWalletId(),
                b.getNickname(),
                b.getBeneficiaryName(),
                b.getIban(),
                b.getBankCode(),
                b.getBankName(),
                b.isVerified(),
                b.isActive(),
                b.getCreatedAt());
    }
}
