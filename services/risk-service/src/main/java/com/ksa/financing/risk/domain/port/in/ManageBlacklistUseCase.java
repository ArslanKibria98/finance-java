package com.ksa.financing.risk.domain.port.in;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.risk.domain.model.MobileBlacklistEntry;
import com.ksa.financing.risk.domain.model.NidBlacklistEntry;

public interface ManageBlacklistUseCase {

    NidBlacklistEntry blacklistNid(String nationalId, String reason);

    NidBlacklistEntry removeNid(String nationalId);

    NidBlacklistEntry getNidStatus(String nationalId);

    PageResponse<NidBlacklistEntry> listNidBlacklist(PageQuery pageQuery);

    MobileBlacklistEntry blacklistMobile(String mobileNumber, String reason);

    MobileBlacklistEntry removeMobile(String mobileNumber);

    MobileBlacklistEntry getMobileStatus(String mobileNumber);

    PageResponse<MobileBlacklistEntry> listMobileBlacklist(PageQuery pageQuery);
}
