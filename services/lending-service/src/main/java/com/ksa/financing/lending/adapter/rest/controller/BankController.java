package com.ksa.financing.lending.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.lending.adapter.rest.response.BankResponse;
import com.ksa.financing.lending.domain.model.Bank;
import com.ksa.financing.lending.domain.port.out.BankRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/banks")
@RequiredArgsConstructor
@Tag(name = "Banks", description = "Bank reference data (same shape as customer bank-accounts)")
public class BankController {

    private final BankRepository bankRepository;

    @SecuredEndpoint(obj = "banks", act = "read")
    @GetMapping
    @Operation(summary = "List all active banks (paginated). Same response shape as /customers/{id}/bank-accounts")
    public PageResponse<BankResponse> listActive(
            PageQuery query,
            @AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        return bankRepository.findAllActive(tenantId, query).map(this::toResponse);
    }

    private BankResponse toResponse(Bank b) {
        String mockIban = generateMockIban(b.code());
        String maskedIban = mockIban != null && mockIban.length() > 4
                ? "****" + mockIban.substring(mockIban.length() - 4)
                : mockIban;
        return new BankResponse(
                b.id(),
                b.nameEn(),
                b.code(),
                b.nameEn(),
                b.nameAr(),
                mockIban,                         // mock IBAN for demo
                maskedIban,
                "Sample Holder",                  // mock account holder
                "CURRENT",
                false,                            // isPrimary
                false,                            // isSalaryAccount
                false,                            // salaryAccount alias
                b.active() ? "ACTIVE" : "INACTIVE",
                null,                             // verifiedAt
                null,                             // createdAt
                b.sortOrder());
    }

    /** Generate a deterministic mock Saudi IBAN: SA<2-check><2-bank>+18 digits. */
    private static String generateMockIban(String bankCode) {
        if (bankCode == null) return null;
        String code2 = bankCode.length() >= 2 ? bankCode.substring(0, 2) : ("0" + bankCode);
        // Deterministic check digits (00) + bank code + 18 padded digits
        String suffix = String.format("%018d", Math.abs((long) bankCode.hashCode()));
        return "SA00" + code2 + suffix;
    }

    private UUID extractTenantId(Jwt jwt) {
        var tenantClaim = jwt.getClaimAsString("tenant_id");
        if (tenantClaim == null) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS,
                    "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenantClaim);
    }
}
