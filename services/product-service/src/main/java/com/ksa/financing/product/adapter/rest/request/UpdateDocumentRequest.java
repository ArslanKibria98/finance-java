package com.ksa.financing.product.adapter.rest.request;

public record UpdateDocumentRequest(
    String nameEn,
    String nameAr,
    String documentType,
    String fileUrl,
    Long fileSizeBytes,
    String fileVersion,
    Boolean required,
    Integer sortOrder
) {}
