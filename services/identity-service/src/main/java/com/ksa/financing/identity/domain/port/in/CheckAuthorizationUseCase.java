package com.ksa.financing.identity.domain.port.in;

public interface CheckAuthorizationUseCase {

    record AuthorizationRequest(String subject, String resource, String action) {}
    record AuthorizationResult(boolean allowed, String subject, String resource, String action) {}

    AuthorizationResult check(AuthorizationRequest request);
}
