package com.ksa.financing.product.adapter.rest.request;

public record UpdatePartnerRequest(
    String nameEn,
    String nameAr,
    String email,
    String phone,
    String contactPerson,
    String logoUrl
) {}
