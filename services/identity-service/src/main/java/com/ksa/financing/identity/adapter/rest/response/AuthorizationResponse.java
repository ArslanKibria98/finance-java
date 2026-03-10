package com.ksa.financing.identity.adapter.rest.response;

public record AuthorizationResponse(
    boolean allowed,
    String subject,
    String resource,
    String action
) {}
