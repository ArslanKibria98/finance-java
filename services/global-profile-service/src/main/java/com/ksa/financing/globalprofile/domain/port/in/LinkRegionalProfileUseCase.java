package com.ksa.financing.globalprofile.domain.port.in;

import com.ksa.financing.globalprofile.domain.model.RegionalProfile;
import java.util.UUID;

public interface LinkRegionalProfileUseCase {
    RegionalProfile link(LinkRegionalProfileCommand command);

    record LinkRegionalProfileCommand(
        UUID globalUid,
        String countryCode,
        String regionalCifNumber,
        UUID piiVaultRecordId,
        UUID keycloakUserId
    ) {}
}
