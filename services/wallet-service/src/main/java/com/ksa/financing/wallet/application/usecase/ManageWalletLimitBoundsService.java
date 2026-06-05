package com.ksa.financing.wallet.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.wallet.domain.model.WalletLimitBounds;
import com.ksa.financing.wallet.domain.port.in.ManageWalletLimitBoundsUseCase;
import com.ksa.financing.wallet.domain.port.out.WalletLimitBoundsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class ManageWalletLimitBoundsService implements ManageWalletLimitBoundsUseCase {

    private final WalletLimitBoundsRepository boundsRepository;
    private final BigDecimal defaultSingle;
    private final BigDecimal defaultDaily;
    private final BigDecimal defaultMonthly;
    private final BigDecimal defaultYearly;
    private final BigDecimal defaultMaxSingle;
    private final BigDecimal defaultMaxDaily;
    private final BigDecimal defaultMaxMonthly;
    private final BigDecimal defaultMaxYearly;

    public ManageWalletLimitBoundsService(
            WalletLimitBoundsRepository boundsRepository,
            @Value("${wallet.limits.default-single:10000}") BigDecimal defaultSingle,
            @Value("${wallet.limits.default-daily:20000}") BigDecimal defaultDaily,
            @Value("${wallet.limits.default-monthly:100000}") BigDecimal defaultMonthly,
            @Value("${wallet.limits.default-yearly:1000000}") BigDecimal defaultYearly,
            @Value("${wallet.limits.max-single:50000}") BigDecimal defaultMaxSingle,
            @Value("${wallet.limits.max-daily:50000}") BigDecimal defaultMaxDaily,
            @Value("${wallet.limits.max-monthly:500000}") BigDecimal defaultMaxMonthly,
            @Value("${wallet.limits.max-yearly:5000000}") BigDecimal defaultMaxYearly) {
        this.boundsRepository = boundsRepository;
        this.defaultSingle = defaultSingle;
        this.defaultDaily = defaultDaily;
        this.defaultMonthly = defaultMonthly;
        this.defaultYearly = defaultYearly;
        this.defaultMaxSingle = defaultMaxSingle;
        this.defaultMaxDaily = defaultMaxDaily;
        this.defaultMaxMonthly = defaultMaxMonthly;
        this.defaultMaxYearly = defaultMaxYearly;
    }

    @Override
    @Transactional
    public WalletLimitBounds getBounds(UUID tenantId) {
        return boundsRepository.findByTenantId(tenantId)
                .orElseGet(() -> boundsRepository.save(defaults(tenantId)));
    }

    @Override
    @Transactional
    public WalletLimitBounds updateBounds(UpdateBoundsCommand cmd) {
        if (cmd.maxSingleLimit().compareTo(cmd.minSingleLimit()) < 0
                || cmd.maxDailyLimit().compareTo(cmd.minDailyLimit()) < 0
                || cmd.maxMonthlyLimit().compareTo(cmd.minMonthlyLimit()) < 0
                || cmd.maxYearlyLimit().compareTo(cmd.minYearlyLimit()) < 0) {
            throw new BusinessException("WALLET.LIMIT_BOUNDS.INVALID_RANGE",
                    "Max limit must be greater than or equal to min limit");
        }
        if (!isWithin(cmd.defaultSingleLimit(), cmd.minSingleLimit(), cmd.maxSingleLimit())
                || !isWithin(cmd.defaultDailyLimit(), cmd.minDailyLimit(), cmd.maxDailyLimit())
                || !isWithin(cmd.defaultMonthlyLimit(), cmd.minMonthlyLimit(), cmd.maxMonthlyLimit())
                || !isWithin(cmd.defaultYearlyLimit(), cmd.minYearlyLimit(), cmd.maxYearlyLimit())) {
            throw new BusinessException("WALLET.LIMIT_BOUNDS.INVALID_DEFAULT",
                    "Default limit must fall within [min, max]");
        }

        WalletLimitBounds bounds = boundsRepository.findByTenantId(cmd.tenantId())
                .orElseGet(() -> defaults(cmd.tenantId()));
        bounds.setMinSingleLimit(cmd.minSingleLimit());
        bounds.setMaxSingleLimit(cmd.maxSingleLimit());
        bounds.setDefaultSingleLimit(cmd.defaultSingleLimit());
        bounds.setMinDailyLimit(cmd.minDailyLimit());
        bounds.setMaxDailyLimit(cmd.maxDailyLimit());
        bounds.setMinMonthlyLimit(cmd.minMonthlyLimit());
        bounds.setMaxMonthlyLimit(cmd.maxMonthlyLimit());
        bounds.setMinYearlyLimit(cmd.minYearlyLimit());
        bounds.setMaxYearlyLimit(cmd.maxYearlyLimit());
        bounds.setDefaultDailyLimit(cmd.defaultDailyLimit());
        bounds.setDefaultMonthlyLimit(cmd.defaultMonthlyLimit());
        bounds.setDefaultYearlyLimit(cmd.defaultYearlyLimit());
        bounds.setUpdatedBy(cmd.updatedBy());
        bounds.setUpdatedAt(Instant.now());
        WalletLimitBounds saved = boundsRepository.save(bounds);
        log.info("Updated wallet limit bounds tenant={} by={} maxDaily={} maxMonthly={}",
                cmd.tenantId(), cmd.updatedBy(), saved.getMaxDailyLimit(), saved.getMaxMonthlyLimit());
        return saved;
    }

    private boolean isWithin(BigDecimal v, BigDecimal min, BigDecimal max) {
        return v.compareTo(min) >= 0 && v.compareTo(max) <= 0;
    }

    private WalletLimitBounds defaults(UUID tenantId) {
        WalletLimitBounds b = new WalletLimitBounds();
        b.setTenantId(tenantId);
        b.setMinSingleLimit(BigDecimal.ZERO);
        b.setMaxSingleLimit(defaultMaxSingle);
        b.setDefaultSingleLimit(defaultSingle);
        b.setMinDailyLimit(BigDecimal.ZERO);
        b.setMaxDailyLimit(defaultMaxDaily);
        b.setMinMonthlyLimit(BigDecimal.ZERO);
        b.setMaxMonthlyLimit(defaultMaxMonthly);
        b.setMinYearlyLimit(BigDecimal.ZERO);
        b.setMaxYearlyLimit(defaultMaxYearly);
        b.setDefaultDailyLimit(defaultDaily);
        b.setDefaultMonthlyLimit(defaultMonthly);
        b.setDefaultYearlyLimit(defaultYearly);
        b.setCreatedAt(Instant.now());
        b.setUpdatedAt(Instant.now());
        return b;
    }
}
