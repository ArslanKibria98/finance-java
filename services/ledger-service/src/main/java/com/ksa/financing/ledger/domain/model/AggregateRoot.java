package com.ksa.financing.ledger.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for all aggregate roots in the ledger domain.
 * Provides domain event registration and collection.
 * Zero framework imports — pure Java.
 */
public abstract class AggregateRoot<ID> {

    private final List<Object> uncommittedEvents = new ArrayList<>();

    public abstract ID getId();

    protected void registerEvent(Object event) {
        uncommittedEvents.add(event);
    }

    public List<Object> getUncommittedEvents() {
        return Collections.unmodifiableList(uncommittedEvents);
    }

    public void markEventsAsCommitted() {
        uncommittedEvents.clear();
    }
}
