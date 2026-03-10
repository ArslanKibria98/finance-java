package com.ksa.financing.identity.application.usecase;

import com.ksa.financing.identity.domain.port.in.CheckAuthorizationUseCase;
import com.ksa.financing.identity.domain.port.out.PolicyEnforcerPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckAuthorizationService implements CheckAuthorizationUseCase {

    private final PolicyEnforcerPort policyEnforcer;

    @Override
    public AuthorizationResult check(AuthorizationRequest request) {
        boolean allowed = policyEnforcer.enforce(request.subject(), request.resource(), request.action());
        log.debug("Authorization check: sub={}, obj={}, act={} => {}",
                request.subject(), request.resource(), request.action(), allowed);
        return new AuthorizationResult(allowed, request.subject(), request.resource(), request.action());
    }
}
