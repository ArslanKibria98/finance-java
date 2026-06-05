package com.ksa.financing.risk.domain.port.out;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.risk.domain.model.BlacklistStatus;
import com.ksa.financing.risk.domain.model.MobileBlacklistEntry;
import com.ksa.financing.risk.domain.model.NidBlacklistEntry;

import java.util.Optional;
import java.util.UUID;

public interface BlacklistRepository {

    NidBlacklistEntry saveNid(NidBlacklistEntry entry);

    Optional<NidBlacklistEntry> findNidByNationalId(String nationalId);

    void updateNidStatus(String nationalId, BlacklistStatus status);

    void updateNidBlockCode(String nationalId, UUID blockCodeId);

    PageResponse<NidBlacklistEntry> findAllNid(PageQuery pageQuery);

    boolean isNidBlacklisted(String nationalId);

    MobileBlacklistEntry saveMobile(MobileBlacklistEntry entry);

    Optional<MobileBlacklistEntry> findMobileByNumber(String mobileNumber);

    void updateMobileStatus(String mobileNumber, BlacklistStatus status);

    void updateMobileBlockCode(String mobileNumber, UUID blockCodeId);

    PageResponse<MobileBlacklistEntry> findAllMobile(PageQuery pageQuery);

    boolean isMobileBlacklisted(String mobileNumber);
}
