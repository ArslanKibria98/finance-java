package com.ksa.financing.ledger.infrastructure.messaging;

import com.ksa.financing.ledger.domain.model.AccountId;
import com.ksa.financing.ledger.domain.port.out.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves standard GL account codes to AccountIds per tenant.
 * Caches results in memory to avoid repeated DB lookups during event processing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LedgerAccountResolver {

    private final AccountRepository accountRepository;
    private final Map<String, UUID> cache = new ConcurrentHashMap<>();

    public AccountId resolve(UUID tenantId, String accountCode) {
        var key = tenantId + ":" + accountCode;
        var cached = cache.get(key);
        if (cached != null) {
            return AccountId.of(cached);
        }
        var account = accountRepository.findByCode(tenantId, accountCode)
                .orElseThrow(() -> new IllegalStateException(
                        "GL account not found for tenant=" + tenantId + " code=" + accountCode +
                        ". Verify COA seed migration has run."));
        cache.put(key, account.getId().value());
        return account.getId();
    }
}
