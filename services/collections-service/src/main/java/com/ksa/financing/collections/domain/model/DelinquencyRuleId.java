package com.ksa.financing.collections.domain.model;

import java.util.UUID;

public record DelinquencyRuleId(UUID value) {
    public DelinquencyRuleId {
        if (value == null) throw new IllegalArgumentException("DelinquencyRuleId must not be null");
    }
    public static DelinquencyRuleId of(UUID id) { return new DelinquencyRuleId(id); }
    public static DelinquencyRuleId generate() { return new DelinquencyRuleId(UUID.randomUUID()); }
    public UUID getValue() { return value; }
}
