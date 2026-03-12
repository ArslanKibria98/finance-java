package com.ksa.financing.product.adapter.rest.request;

import java.util.List;
import java.util.UUID;

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
    String logoUrl,
    UUID countryId
) {}
