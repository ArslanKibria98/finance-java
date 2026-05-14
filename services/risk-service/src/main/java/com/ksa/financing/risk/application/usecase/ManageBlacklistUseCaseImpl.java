package com.ksa.financing.risk.application.usecase;

import com.ksa.financing.domain.valueobject.NationalId;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.risk.domain.model.BlacklistStatus;
import com.ksa.financing.risk.domain.model.MobileBlacklistEntry;
import com.ksa.financing.risk.domain.model.NidBlacklistEntry;
import com.ksa.financing.risk.domain.port.in.ManageBlacklistUseCase;
import com.ksa.financing.risk.domain.port.out.BlacklistRepository;
import com.ksa.financing.risk.infrastructure.blacklist.BlacklistRedisSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageBlacklistUseCaseImpl implements ManageBlacklistUseCase {

    private final BlacklistRepository blacklistRepository;
    private final BlacklistRedisSyncService redisSync;

    @Override
    @Transactional
    public NidBlacklistEntry blacklistNid(String nationalId, String reason, UUID blockCodeId) {
        var existing = blacklistRepository.findNidByNationalId(nationalId);
        if (existing.isPresent() && existing.get().status() == BlacklistStatus.BLACKLISTED) {
            throw new BusinessException("RISK.BLACKLIST.ALREADY_BLACKLISTED",
                "National ID is already blacklisted: " + maskNid(nationalId));
        }

        if (existing.isPresent()) {
            blacklistRepository.updateNidStatus(nationalId, BlacklistStatus.BLACKLISTED);
            if (blockCodeId != null) {
                blacklistRepository.updateNidBlockCode(nationalId, blockCodeId);
            }
            log.info("NID re-blacklisted: {}", maskNid(nationalId));
            redisSync.onNidBlacklisted(nationalId, reason);
            return blacklistRepository.findNidByNationalId(nationalId).orElseThrow();
        }

        var entry = new NidBlacklistEntry(null, NationalId.of(nationalId), reason, BlacklistStatus.BLACKLISTED,
            null, blockCodeId, null, Instant.now(), Instant.now());
        var saved = blacklistRepository.saveNid(entry);
        log.info("NID blacklisted: {}", maskNid(nationalId));
        redisSync.onNidBlacklisted(nationalId, reason);
        return saved;
    }

    @Override
    @Transactional
    public NidBlacklistEntry removeNid(String nationalId) {
        blacklistRepository.findNidByNationalId(nationalId)
            .orElseThrow(() -> NotFoundException.forEntity("NidBlacklist", nationalId));
        blacklistRepository.updateNidStatus(nationalId, BlacklistStatus.REMOVED);
        log.info("NID removed from blacklist: {}", maskNid(nationalId));
        redisSync.onNidRemoved(nationalId);
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
    public PageResponse<NidBlacklistEntry> listNidBlacklist(PageQuery pageQuery) {
        return blacklistRepository.findAllNid(pageQuery);
    }

    @Override
    @Transactional
    public NidBlacklistEntry assignNidBlockCode(String nationalId, UUID blockCodeId) {
        blacklistRepository.findNidByNationalId(nationalId)
            .orElseThrow(() -> NotFoundException.forEntity("NidBlacklist", nationalId));
        blacklistRepository.updateNidBlockCode(nationalId, blockCodeId);
        log.info("Block code {} assigned to NID: {}", blockCodeId, maskNid(nationalId));
        return blacklistRepository.findNidByNationalId(nationalId).orElseThrow();
    }

    @Override
    @Transactional
    public MobileBlacklistEntry blacklistMobile(String mobileNumber, String reason, UUID blockCodeId) {
        var existing = blacklistRepository.findMobileByNumber(mobileNumber);
        if (existing.isPresent() && existing.get().status() == BlacklistStatus.BLACKLISTED) {
            throw new BusinessException("RISK.BLACKLIST.ALREADY_BLACKLISTED",
                "Mobile number is already blacklisted: " + maskMobile(mobileNumber));
        }

        if (existing.isPresent()) {
            blacklistRepository.updateMobileStatus(mobileNumber, BlacklistStatus.BLACKLISTED);
            if (blockCodeId != null) {
                blacklistRepository.updateMobileBlockCode(mobileNumber, blockCodeId);
            }
            log.info("Mobile re-blacklisted: {}", maskMobile(mobileNumber));
            redisSync.onMobileBlacklisted(mobileNumber, reason);
            return blacklistRepository.findMobileByNumber(mobileNumber).orElseThrow();
        }

        var entry = new MobileBlacklistEntry(null, mobileNumber, reason, BlacklistStatus.BLACKLISTED,
            null, blockCodeId, null, Instant.now(), Instant.now());
        var saved = blacklistRepository.saveMobile(entry);
        log.info("Mobile blacklisted: {}", maskMobile(mobileNumber));
        redisSync.onMobileBlacklisted(mobileNumber, reason);
        return saved;
    }

    @Override
    @Transactional
    public MobileBlacklistEntry removeMobile(String mobileNumber) {
        blacklistRepository.findMobileByNumber(mobileNumber)
            .orElseThrow(() -> NotFoundException.forEntity("MobileBlacklist", mobileNumber));
        blacklistRepository.updateMobileStatus(mobileNumber, BlacklistStatus.REMOVED);
        log.info("Mobile removed from blacklist: {}", maskMobile(mobileNumber));
        redisSync.onMobileRemoved(mobileNumber);
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
    public PageResponse<MobileBlacklistEntry> listMobileBlacklist(PageQuery pageQuery) {
        return blacklistRepository.findAllMobile(pageQuery);
    }

    @Override
    @Transactional
    public MobileBlacklistEntry assignMobileBlockCode(String mobileNumber, UUID blockCodeId) {
        blacklistRepository.findMobileByNumber(mobileNumber)
            .orElseThrow(() -> NotFoundException.forEntity("MobileBlacklist", mobileNumber));
        blacklistRepository.updateMobileBlockCode(mobileNumber, blockCodeId);
        log.info("Block code {} assigned to mobile: {}", blockCodeId, maskMobile(mobileNumber));
        return blacklistRepository.findMobileByNumber(mobileNumber).orElseThrow();
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
