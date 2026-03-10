package com.ksa.financing.globalprofile.domain.port.in;

import com.ksa.financing.globalprofile.domain.model.GlobalCustomer;

public interface CreateGlobalProfileUseCase {
    GlobalCustomer create(CreateGlobalProfileCommand command);

    record CreateGlobalProfileCommand(
        String email,
        String mobile,
        String primaryCountryCode
    ) {}
}
