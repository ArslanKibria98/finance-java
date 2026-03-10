package com.ksa.financing.piivault.domain.port.in;

import java.util.UUID;

public interface DeletePiiUseCase {
    void delete(UUID globalUid, UUID deletedBy, String reason);
}
