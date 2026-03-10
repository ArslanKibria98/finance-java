package com.ksa.financing.globalprofile.domain.port.in;

import com.ksa.financing.globalprofile.domain.model.PiiAccessToken;
import java.util.List;
import java.util.UUID;

public interface IssuePiiAccessTokenUseCase {
    PiiAccessToken issue(IssuePiiAccessTokenCommand command);

    record IssuePiiAccessTokenCommand(
        UUID globalUid,
        List<String> allowedFields,
        UUID requesterId,
        String requesterRole,
        String requesterIp,
        String accessPurpose,
        String relatedEntityType,
        UUID relatedEntityId
    ) {}
}
