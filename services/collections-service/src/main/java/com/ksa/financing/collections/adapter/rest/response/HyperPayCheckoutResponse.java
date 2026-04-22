package com.ksa.financing.collections.adapter.rest.response;

import java.math.BigDecimal;
import java.util.UUID;

public record HyperPayCheckoutResponse(
        UUID paymentId,
        String checkoutId,
        BigDecimal amount,
        String currency,
        String paymentBrand,
        String status,
        String widgetUrl
) {}
