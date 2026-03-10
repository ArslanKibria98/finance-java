package com.ksa.financing.identity.adapter.rest.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UpdateEmployeeRequest(
    String name,
    String phone,
    String address,
    UUID roleId,
    Boolean active,
    String status
) {}
