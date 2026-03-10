package com.ksa.financing.identity.domain.port.in;

import com.ksa.financing.identity.domain.model.UserSession;
import java.util.List;
import java.util.UUID;

public interface ManageSessionUseCase {
    List<UserSession> getActiveSessions(UUID userIdentityId);
    void revokeSession(UUID sessionId);
    void revokeAllSessions(UUID userIdentityId);
}
