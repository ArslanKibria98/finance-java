package com.ksa.financing.risk.domain.port.in;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.risk.domain.model.MobileBlacklistEntry;
import com.ksa.financing.risk.domain.model.NidBlacklistEntry;

import java.util.UUID;

public interface ManageBlacklistUseCase {

    NidBlacklistEntry blacklistNid(String nationalId, String reason, UUID blockCodeId);

    NidBlacklistEntry removeNid(String nationalId);

    NidBlacklistEntry getNidStatus(String nationalId);

    PageResponse<NidBlacklistEntry> listNidBlacklist(PageQuery pageQuery);

    NidBlacklistEntry assignNidBlockCode(String nationalId, UUID blockCodeId);

    MobileBlacklistEntry blacklistMobile(String mobileNumber, String reason, UUID blockCodeId);

    MobileBlacklistEntry removeMobile(String mobileNumber);

    MobileBlacklistEntry getMobileStatus(String mobileNumber);

    PageResponse<MobileBlacklistEntry> listMobileBlacklist(PageQuery pageQuery);

    MobileBlacklistEntry assignMobileBlockCode(String mobileNumber, UUID blockCodeId);
}
