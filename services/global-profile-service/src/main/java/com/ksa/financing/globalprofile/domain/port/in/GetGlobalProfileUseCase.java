package com.ksa.financing.globalprofile.domain.port.in;

import com.ksa.financing.globalprofile.domain.model.GlobalCustomer;
import com.ksa.financing.globalprofile.domain.model.RegionalProfile;
import java.util.List;
import java.util.UUID;

public interface GetGlobalProfileUseCase {
    GlobalCustomer getByGlobalUid(UUID globalUid);
    GlobalCustomer findByEmail(String email);
    GlobalCustomer findByMobile(String mobile);
    List<RegionalProfile> getRegionalProfiles(UUID globalUid);
}
