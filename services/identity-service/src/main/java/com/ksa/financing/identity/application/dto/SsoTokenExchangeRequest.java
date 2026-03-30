package com.ksa.financing.identity.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Frontend yeh request bhejta hai jab Keycloak code ke saath redirect karta hai.
 * - code:  Keycloak ne URL mein diya (?code=xxx)
 * - state: Wahi state jo /sso/login-url se mili thi
 */
public record SsoTokenExchangeRequest(
        @NotBlank(message = "Authorization code is required")
        String code,

        @NotBlank(message = "State is required")
        String state,

        String redirectUri
) {}
