package com.ksa.financing.piivault.domain.port.out;

import com.ksa.financing.piivault.domain.model.PiiIndividual;
import java.util.Optional;
import java.util.UUID;

public interface PiiRepository {
    PiiIndividual save(PiiIndividual piiIndividual);
    Optional<PiiIndividual> findByGlobalUid(UUID globalUid);
    void deleteByGlobalUid(UUID globalUid);
}
