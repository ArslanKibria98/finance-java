package com.ksa.financing.identity.domain.port.out;

import com.ksa.financing.identity.domain.model.UserIdentity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserIdentityRepository {
    UserIdentity save(UserIdentity userIdentity);
    Optional<UserIdentity> findById(UUID id);
    Optional<UserIdentity> findByKeycloakUserId(UUID keycloakUserId);
    Optional<UserIdentity> findByInternalUserId(UUID tenantId, UUID internalUserId);
    Optional<UserIdentity> findByInternalCustomerId(UUID customerId);
    Optional<UserIdentity> findByKeycloakUsername(String keycloakUsername);
    Optional<UserIdentity> findByMobileNumber(String mobileNumber);
    void saveOtp(UUID id, String otp, Instant expiry);
    void clearOtp(UUID id);
}
