package com.ksa.financing.wallet.adapter.rest.controller;

import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.port.out.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Internal endpoint for service-to-service wallet lookups.
 * No JWT required — used by customer-service for profile API.
 */
@RestController
@RequestMapping("/internal/wallets")
@RequiredArgsConstructor
@Slf4j
public class InternalWalletController {

    private final WalletRepository walletRepository;

    @GetMapping("/by-customer/{customerId}")
    public ResponseEntity<WalletIbanResponse> getWalletIbanByCustomerId(
            @PathVariable UUID customerId,
            @RequestHeader(value = "X-Tenant-Id") String tenantId) {

        log.info("Internal: Getting wallet IBAN for customer: {} tenant: {}", customerId, tenantId);

        UUID tenantUuid;
        try {
            tenantUuid = UUID.fromString(tenantId);
        } catch (IllegalArgumentException e) {
            tenantUuid = UUID.nameUUIDFromBytes(tenantId.getBytes());
        }

        return walletRepository.findByCustomerId(tenantUuid, customerId)
                .map(wallet -> ResponseEntity.ok(new WalletIbanResponse(
                        wallet.getId(),
                        wallet.getCustomerId(),
                        wallet.getWalletNumber(),
                        wallet.getIban()
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    public record WalletIbanResponse(
            UUID walletId,
            UUID customerId,
            String walletNumber,
            String iban
    ) {}
}
