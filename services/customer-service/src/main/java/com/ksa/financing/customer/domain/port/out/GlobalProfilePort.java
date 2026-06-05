package com.ksa.financing.customer.domain.port.out;

import java.util.UUID;

public interface GlobalProfilePort {
    UUID createGlobalProfile(String email, String mobile, String countryCode);
    UUID findByEmail(String email);
}
