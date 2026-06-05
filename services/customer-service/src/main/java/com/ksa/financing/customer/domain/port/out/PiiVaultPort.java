package com.ksa.financing.customer.domain.port.out;

import java.util.Map;
import java.util.UUID;

public interface PiiVaultPort {
    UUID storePii(UUID globalUid, Map<String, String> piiFields);
    Map<String, String> retrievePii(UUID globalUid, String accessToken);
    void deletePii(UUID globalUid);
}
