package com.ksa.financing.identity.application.mapper;

import com.ksa.financing.identity.application.dto.RegisterResponse;
import com.ksa.financing.identity.application.dto.UserIdentityResponse;
import com.ksa.financing.identity.domain.model.UserIdentity;

public class UserIdentityMapper {

    private UserIdentityMapper() {}

    public static RegisterResponse toRegisterResponse(UserIdentity identity) {
        return new RegisterResponse(
            identity.getId(),
            identity.getKeycloakUserId(),
            identity.getKeycloakUsername(),
            identity.getStatus().name()
        );
    }

    public static UserIdentityResponse toResponse(UserIdentity identity) {
        return new UserIdentityResponse(
            identity.getId(),
            identity.getKeycloakUserId(),
            identity.getKeycloakUsername(),
            identity.getUserType().name(),
            identity.getStatus().name(),
            identity.getGlobalUid(),
            identity.getCreatedAt()
        );
    }
}
