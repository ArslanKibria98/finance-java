package com.ksa.financing.middleware.adapter.mock;

import com.ksa.financing.middleware.adapter.mock.provider.MockResponseProvider;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class MockResponseDispatcher {

    private final List<MockResponseProvider> providers;
    private Map<String, MockResponseProvider> providerMap;

    @PostConstruct
    void init() {
        providerMap = providers.stream()
                .collect(Collectors.toMap(MockResponseProvider::getProviderCode, p -> p));
        log.info("MockResponseDispatcher initialized with {} providers: {}",
                providerMap.size(), providerMap.keySet());
    }

    public MockResponseResult dispatch(String providerCode, String apiCode, String requestBody) {
        var provider = providerMap.get(providerCode);
        if (provider == null) {
            log.warn("No mock provider registered for: {}", providerCode);
            return defaultResponse(providerCode, apiCode);
        }
        return provider.getMockResponse(apiCode, requestBody);
    }

    private MockResponseResult defaultResponse(String providerCode, String apiCode) {
        var body = """
                {
                    "success": true,
                    "message": "Mock: No handler registered for provider %s, API %s",
                    "data": {},
                    "current_timestamp": "%s"
                }
                """.formatted(providerCode, apiCode, Instant.now().toString());
        return new MockResponseResult(200, body, "Content-Type: application/json");
    }
}
