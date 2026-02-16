package com.ksa.financing.service.template.domain.model;

import lombok.Getter;
import lombok.AllArgsConstructor;
import com.ksa.financing.domain.base.Entity;

/**
 * Example Entity within the ExampleAggregate boundary.
 * Entities have identity but are not accessible outside the aggregate.
 */
@Getter
@AllArgsConstructor
public class ExampleEntity extends Entity<String> {

    private final String id;
    private final String name;
    private final String value;

    /**
     * Business logic specific to this entity.
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty()
            && value != null && !value.trim().isEmpty();
    }

    @Override
    public String getId() {
        return id;
    }
}