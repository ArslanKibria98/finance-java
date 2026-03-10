package com.ksa.financing.identity.application.dto;

/**
 * Response returned to frontend containing:
 * - authUrl: Keycloak login page URL jahan frontend redirect kare ga
 * - state:   Random UUID jo PKCE verifier se linked hai (Redis mein store)
 *            Frontend isko /sso/token call mein wapas bheje ga
 */
public record SsoLoginUrlResponse(
        String authUrl,
        String state
) {}
