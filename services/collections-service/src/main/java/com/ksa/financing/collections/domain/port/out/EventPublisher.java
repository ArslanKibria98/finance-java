package com.ksa.financing.collections.domain.port.out;

import java.util.List;

public interface EventPublisher {

    void publishAll(List<Object> events);
}
