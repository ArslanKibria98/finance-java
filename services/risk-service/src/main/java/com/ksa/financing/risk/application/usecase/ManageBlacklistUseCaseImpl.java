package com.ksa.financing.risk.application.usecase;

import com.ksa.financing.domain.valueobject.NationalId;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.risk.domain.model.BlacklistStatus;
import com.ksa.financing.risk.domain.model.MobileBlacklistEntry;
import com.ksa.financing.risk.domain.model.NidBlacklistEntry;
import com.ksa.financing.risk.domain.port.in.ManageBlacklistUseCase;
import com.ksa.financing.risk.domain.port.out.BlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageBlacklistUseCaseImpl implements ManageBlacklistUseCase {

    private final BlacklistRepository blacklistRepository;

    @Override
    @Transactional
    public NidBlacklistEntry blacklistNid(String nationalId, String reason) {
        var existing = blacklistRepository.findNidByNationalId(nationalId);
        if (existing.isPresent() && existing.get().status() == BlacklistStatus.BLACKLISTED) {
            throw new BusinessException("RISK.BLACKLIST.ALREADY_BLACKLISTED",
                "National ID is already blacklisted: " + maskNid(nationalId));
        }

        if (existing.isPresent()) {
            blacklistRepository.updateNidStatus(nationalId, BlacklistStatus.BLACKLISTED);
            log.info("NID re-blacklisted: {}", maskNid(nationalId));
            return blacklistRepository.findNidByNationalId(nationalId).orElseThrow();
        }

        var entry = new NidBlacklistEntry(null, NationalId.of(nationalId), reason, BlacklistStatus.BLACKLISTED,
            null, Instant.now(), Instant.now());
        var saved = blacklistRepository.saveNid(entry);
        log.info("NID blacklisted: {}", maskNid(nationalId));
        return saved;
    }

    @Override
    @Transactional
    public NidBlacklistEntry removeNid(String nationalId) {
        blacklistRepository.findNidByNationalId(nationalId)
            .orElseThrow(() -> NotFoundException.forEntity("NidBlacklist", nationalId));
        blacklistRepository.updateNidStatus(nationalId, BlacklistStatus.REMOVED);
        log.info("NID removed from blacklist: {}", maskNid(nationalId));
        return blacklistRepository.findNidByNationalId(nationalId).orElseThrow();
    }

    @Override
    @Transactional(readOnly = true)
    public NidBlacklistEntry getNidStatus(String nationalId) {
        return blacklistRepository.findNidByNationalId(nationalId)
            .orElseThrow(() -> NotFoundException.forEntity("NidBlacklist", nationalId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NidBlacklistEntry> listNidBlacklist() {
        return blacklistRepository.findAllNid();
    }

    @Override
    @Transactional
    public MobileBlacklistEntry blacklistMobile(String mobileNumber, String reason) {
        var existing = blacklistRepository.findMobileByNumber(mobileNumber);
        if (existing.isPresent() && existing.get().status() == BlacklistStatus.BLACKLISTED) {
            throw new BusinessException("RISK.BLACKLIST.ALREADY_BLACKLISTED",
                "Mobile number is already blacklisted: " + maskMobile(mobileNumber));
        }

        if (existing.isPresent()) {
            blacklistRepository.updateMobileStatus(mobileNumber, BlacklistStatus.BLACKLISTED);
            log.info("Mobile re-blacklisted: {}", maskMobile(mobileNumber));
            return blacklistRepository.findMobileByNumber(mobileNumber).orElseThrow();
        }

        var entry = new MobileBlacklistEntry(null, mobileNumber, reason, BlacklistStatus.BLACKLISTED,
            null, Instant.now(), Instant.now());
        var saved = blacklistRepository.saveMobile(entry);
        log.info("Mobile blacklisted: {}", maskMobile(mobileNumber));
        return saved;
    }

    @Override
    @Transactional
    public MobileBlacklistEntry removeMobile(String mobileNumber) {
        blacklistRepository.findMobileByNumber(mobileNumber)
            .orElseThrow(() -> NotFoundException.forEntity("MobileBlacklist", mobileNumber));
        blacklistRepository.updateMobileStatus(mobileNumber, BlacklistStatus.REMOVED);
        log.info("Mobile removed from blacklist: {}", maskMobile(mobileNumber));
        return blacklistRepository.findMobileByNumber(mobileNumber).orElseThrow();
    }

    @Override
    @Transactional(readOnly = true)
    public MobileBlacklistEntry getMobileStatus(String mobileNumber) {
        return blacklistRepository.findMobileByNumber(mobileNumber)
            .orElseThrow(() -> NotFoundException.forEntity("MobileBlacklist", mobileNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MobileBlacklistEntry> listMobileBlacklist() {
        return blacklistRepository.findAllMobile();
    }

    private String maskNid(String nid) {
        try {
            return NationalId.of(nid).toMaskedString();
        } catch (Exception e) {
            return "****";
        }
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4) return "****";
        return "****" + mobile.substring(mobile.length() - 4);
    }
}
