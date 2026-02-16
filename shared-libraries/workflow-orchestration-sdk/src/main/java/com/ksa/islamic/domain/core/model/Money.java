package com.ksa.islamic.domain.core.model;

import lombok.Value;
import java.math.BigDecimal;
import java.util.Currency;

/**
 * Temporary Money class for compilation
 * Replace with actual domain-core-sdk Money class when available
 */
@Value
public class Money {
    BigDecimal amount;
    Currency currency;

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }
}