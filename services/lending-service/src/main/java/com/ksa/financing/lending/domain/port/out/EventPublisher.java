package com.ksa.financing.lending.domain.port.out;

import java.util.List;

public interface EventPublisher {

    void publish(Object event);

    void publishAll(List<Object> events);
}
