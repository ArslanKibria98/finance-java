package com.ksa.financing.identity.application.usecase;

import com.ksa.financing.identity.domain.model.UserIdentity;
import com.ksa.financing.identity.domain.model.UserStatus;
import com.ksa.financing.identity.domain.model.UserType;
import com.ksa.financing.identity.domain.port.in.RegisterUserUseCase;
import com.ksa.financing.identity.domain.port.out.EventPublisherPort;
import com.ksa.financing.identity.domain.port.out.KeycloakAdapterPort;
import com.ksa.financing.identity.domain.port.out.UserIdentityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RegisterUserService implements RegisterUserUseCase {

    private final KeycloakAdapterPort keycloakAdapter;
    private final UserIdentityRepository userIdentityRepository;
    private final EventPublisherPort eventPublisher;

    public RegisterUserService(KeycloakAdapterPort keycloakAdapter,
                               UserIdentityRepository userIdentityRepository,
                               EventPublisherPort eventPublisher) {
        this.keycloakAdapter = keycloakAdapter;
        this.userIdentityRepository = userIdentityRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public UserIdentity register(RegisterUserCommand command) {
        String realm = command.keycloakRealm() != null ? command.keycloakRealm() : "CompanyRealm";

        KeycloakAdapterPort.KeycloakUser keycloakUser = keycloakAdapter.createUser(
            realm, command.username(), command.email(), command.password()
        );

        keycloakAdapter.assignRole(realm, keycloakUser.keycloakUserId(), "customer");

        UserIdentity identity = new UserIdentity();
        identity.setTenantId(command.tenantId());
        identity.setKeycloakUserId(keycloakUser.keycloakUserId());
        identity.setKeycloakRealm(realm);
        identity.setKeycloakUsername(command.username());
        identity.setInternalUserId(UUID.randomUUID());
        identity.setUserType(UserType.CUSTOMER);
        identity.setStatus(UserStatus.PENDING_VERIFICATION);

        UserIdentity saved = userIdentityRepository.save(identity);
        eventPublisher.publishUserRegistered(saved.getId(), saved.getTenantId());

        return saved;
    }
}
