package com.ksa.financing.product.adapter.rest.request;

import java.util.List;

public record UpdateBasicInfoRequest(
    String nameEn,
    String nameAr,
    String descriptionEn,
    String descriptionAr,
    String shortDescriptionEn,
    String shortDescriptionAr,
    String notificationEmail,
    List<String> customerTypes,
    boolean involvesCommodity,
    String logoUrl
) {}
