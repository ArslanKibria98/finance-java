package com.ksa.financing.identity.adapter.rest.response;

public record PolicyResponse(
    String role,
    String resource,
    String action
) {}
