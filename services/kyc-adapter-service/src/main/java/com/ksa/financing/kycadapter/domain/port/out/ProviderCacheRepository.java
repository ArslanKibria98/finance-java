package com.ksa.financing.kycadapter.domain.port.out;

import com.ksa.financing.kycadapter.domain.model.KycProvider;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ProviderCacheRepository {
    Optional<Map<String, Object>> findCachedResponse(String cacheKey);
    void cacheResponse(String cacheKey, KycProvider provider, String subjectId, Map<String, Object> response, int ttlHours);
    Optional<Map<String, Object>> findCachedResponse(UUID tenantId, String cacheKey);
    void cacheResponse(UUID tenantId, String cacheKey, KycProvider provider, String subjectId, Map<String, Object> response, int ttlHours);
}
