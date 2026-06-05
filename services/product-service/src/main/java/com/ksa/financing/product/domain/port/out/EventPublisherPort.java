package com.ksa.financing.product.domain.port.out;

import com.ksa.financing.product.domain.model.Product;
import java.util.UUID;

public interface EventPublisherPort {
    void publishProductCreated(Product product);
    void publishProductActivated(Product product);
    void publishProductUpdated(Product product);
    void publishFeeSettingsUpdated(UUID tenantId, UUID productId, String productCode, Integer maxPenaltyWaiversAllowed, Boolean penaltyWaiverAllowed);
}
