package com.ksa.financing.piivault.domain.port.in;

import com.ksa.financing.piivault.domain.model.PiiIndividual;
import java.util.List;
import java.util.UUID;

public interface RetrievePiiUseCase {
    PiiIndividual retrieve(UUID globalUid, UUID accessorId, String accessorRole, String accessorIp, String accessPurpose, List<String> fields);
}
