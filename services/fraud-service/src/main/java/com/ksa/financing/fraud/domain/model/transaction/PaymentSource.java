package com.ksa.financing.fraud.domain.model.transaction;

public record PaymentSource(
    String iban,
    String cardLast4,
    String cardCountry,
    String cardHolderName,
    boolean thirdParty
) {}
