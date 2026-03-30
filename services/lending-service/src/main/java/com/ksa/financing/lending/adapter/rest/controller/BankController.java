package com.ksa.financing.lending.adapter.rest.controller;

import com.ksa.financing.infra.authorization.SecuredEndpoint;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.lending.domain.model.Bank;
import com.ksa.financing.lending.domain.port.out.BankRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/banks")
@RequiredArgsConstructor
@Tag(name = "Banks", description = "Bank reference data for loan disbursement")
public class BankController {

    private final BankRepository bankRepository;

    @SecuredEndpoint(obj = "banks", act = "read")
    @GetMapping
    @Operation(summary = "List all active banks")
    public ResponseEntity<List<BankResponse>> listBanks(@AuthenticationPrincipal Jwt jwt) {
        var tenantId = extractTenantId(jwt);
        var banks = bankRepository.findAllActive(tenantId);
        var responses = banks.stream().map(BankResponse::from).toList();
        return ResponseEntity.ok(responses);
    }

    public record BankResponse(
            String bankName,
            String bankCode,
            String iban,
            String accountHolderName,
            String accountType,
            boolean salaryAccount,
            String status
    ) {
        public static BankResponse from(Bank bank) {
            return new BankResponse(
                    bank.nameEn(),
                    bank.code(),
                    generateRandomIban(bank.code()),
                    null,
                    "CURRENT",
                    false,
                    "ACTIVE"
            );
        }

        private static String generateRandomIban(String bankCode) {
            var rng = new java.util.Random();
            var sb = new StringBuilder("SA");
            sb.append(String.format("%02d", rng.nextInt(100)));
            sb.append(String.format("%2s", bankCode).replace(' ', '0'));
            for (int i = 0; i < 18; i++) sb.append(rng.nextInt(10));
            return sb.toString();
        }
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
