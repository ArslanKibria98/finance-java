package com.ksa.financing.identity.domain.port.in;

import com.ksa.financing.identity.application.dto.SsoLoginUrlResponse;
import com.ksa.financing.identity.application.dto.SsoTokenExchangeRequest;
import com.ksa.financing.identity.application.dto.SsoTokenResponse;

/**
 * SSO Use Case — Authorization Code Flow with PKCE
 *
 * Step 1: generateLoginUrl()
 *   → PKCE pair banao, verifier Redis mein store karo
 *   → Keycloak auth URL return karo frontend ko
 *
 * Step 2: exchangeToken(request)
 *   → Redis se verifier nikalo (state key se)
 *   → Keycloak token endpoint pe code + verifier bhejo
 *   → Access token + roles return karo
 */
public interface SsoUseCase {

    SsoLoginUrlResponse generateLoginUrl();

    SsoTokenResponse exchangeToken(SsoTokenExchangeRequest request);
}
