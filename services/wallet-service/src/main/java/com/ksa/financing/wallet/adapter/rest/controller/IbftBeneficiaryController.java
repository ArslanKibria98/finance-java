package com.ksa.financing.wallet.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.wallet.adapter.rest.support.AuthWalletResolver;
import com.ksa.financing.wallet.application.dto.IbftBeneficiaryDtos.AddIbftBeneficiaryRequest;
import com.ksa.financing.wallet.application.dto.IbftBeneficiaryDtos.IbftBeneficiaryResponse;
import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.port.in.ManageIbftBeneficiaryUseCase;
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
    private final AuthWalletResolver auth;

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
                request.institutionNumber(), request.transit(), request.accountNumber(),
                request.bankName(), request.currency()));
        return ResponseEntity.status(HttpStatus.CREATED).body(IbftBeneficiaryResponse.from(b));
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
