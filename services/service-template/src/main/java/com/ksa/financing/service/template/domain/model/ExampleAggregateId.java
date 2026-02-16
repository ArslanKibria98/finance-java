package com.ksa.financing.service.template.domain.model;

import com.ksa.financing.domain.base.EntityId;
import java.util.UUID;

/**
 * Value Object representing the identity of an ExampleAggregate.
 */
public class ExampleAggregateId extends EntityId {

    public ExampleAggregateId(String value) {
        super(value);
    }

    public static ExampleAggregateId generate() {
        return new ExampleAggregateId("EXA-" + UUID.randomUUID().toString());
    }

    public static ExampleAggregateId of(String value) {
        if (value == null || !value.startsWith("EXA-")) {
            throw new IllegalArgumentException(
                "Invalid ExampleAggregateId format: " + value
            );
        }
        return new ExampleAggregateId(value);
    }
}