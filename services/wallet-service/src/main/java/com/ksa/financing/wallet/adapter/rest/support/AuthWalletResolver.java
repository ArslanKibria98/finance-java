package com.ksa.financing.wallet.adapter.rest.support;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.port.in.GetBalanceUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Resolves the authenticated customer's own wallet from the JWT (via the {@code mobile_number}
 * claim) — used by IBFT endpoints so the debtor/owner is always the logged-in user, never a body param.
 */
@Component
@RequiredArgsConstructor
public class AuthWalletResolver {

    private final GetBalanceUseCase getBalanceUseCase;

    public UUID tenantId(Jwt jwt) {
        String tenant = jwt.getClaimAsString("tenant_id");
        if (tenant == null) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS, "No tenant_id claim found in JWT token");
        }
        return UUID.fromString(tenant);
    }

    public UUID userId(Jwt jwt) {
        try { return jwt.getSubject() != null ? UUID.fromString(jwt.getSubject()) : null; }
        catch (IllegalArgumentException e) { return null; }
    }

    /** The authenticated user's own wallet (debtor), resolved from the JWT mobile_number claim. */
    public Wallet wallet(Jwt jwt) {
        UUID tenant = tenantId(jwt);
        String mobile = jwt.getClaimAsString("mobile_number");
        if (mobile == null || mobile.isBlank()) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS,
                    "No mobile_number claim in JWT — cannot resolve your wallet");
        }
        return getBalanceUseCase.getByMobile(tenant, mobile);
    }
}
