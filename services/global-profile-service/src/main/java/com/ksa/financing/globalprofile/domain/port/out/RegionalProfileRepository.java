package com.ksa.financing.globalprofile.domain.port.out;

import com.ksa.financing.globalprofile.domain.model.RegionalProfile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RegionalProfileRepository {
    RegionalProfile save(RegionalProfile regionalProfile);
    Optional<RegionalProfile> findById(UUID regionalProfileId);
    List<RegionalProfile> findByGlobalUid(UUID globalUid);
    Optional<RegionalProfile> findByGlobalUidAndCountry(UUID globalUid, String countryCode);
    Optional<RegionalProfile> findByCifNumber(String countryCode, String cifNumber);
}
