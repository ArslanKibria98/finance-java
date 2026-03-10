package com.ksa.financing.risk.domain.port.in;

import com.ksa.financing.risk.domain.model.MobileBlacklistEntry;
import com.ksa.financing.risk.domain.model.NidBlacklistEntry;

import java.util.List;

public interface ManageBlacklistUseCase {

    NidBlacklistEntry blacklistNid(String nationalId, String reason);

    NidBlacklistEntry removeNid(String nationalId);

    NidBlacklistEntry getNidStatus(String nationalId);

    List<NidBlacklistEntry> listNidBlacklist();

    MobileBlacklistEntry blacklistMobile(String mobileNumber, String reason);

    MobileBlacklistEntry removeMobile(String mobileNumber);

    MobileBlacklistEntry getMobileStatus(String mobileNumber);

    List<MobileBlacklistEntry> listMobileBlacklist();
}
