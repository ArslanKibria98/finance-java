package com.ksa.financing.product.adapter.rest.request;

import jakarta.validation.constraints.NotBlank;

public record AddDocumentRequest(
    @NotBlank String nameEn,
    String nameAr,
    @NotBlank String documentType,
    String fileUrl,
    Long fileSizeBytes,
    String fileVersion,
    String createdByName,
    boolean required
) {}
