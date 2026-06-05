package com.ksa.financing.wallet.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.wallet.application.support.TransactionLimitEnforcer;
import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.model.WalletLimitBounds;
import com.ksa.financing.wallet.domain.port.in.GetWalletLimitUseCase;
import com.ksa.financing.wallet.domain.port.out.WalletLimitBoundsRepository;
import com.ksa.financing.wallet.domain.port.out.WalletRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Slf4j
@Service
public class GetWalletLimitService implements GetWalletLimitUseCase {

    private final WalletRepository walletRepository;
    private final WalletLimitBoundsRepository boundsRepository;
    private final TransactionLimitEnforcer limitEnforcer;
    private final ZoneId zone;

    public GetWalletLimitService(
            WalletRepository walletRepository,
            WalletLimitBoundsRepository boundsRepository,
            TransactionLimitEnforcer limitEnforcer,
            @Value("${wallet.limits.timezone:Asia/Riyadh}") String timezone) {
        this.walletRepository = walletRepository;
        this.boundsRepository = boundsRepository;
        this.limitEnforcer = limitEnforcer;
        this.zone = ZoneId.of(timezone);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletLimitView getLimit(UUID tenantId, UUID walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> NotFoundException.forEntity("Wallet", walletId.toString()));
        if (!wallet.getTenantId().equals(tenantId)) {
            throw new BusinessException(ErrorCodes.INVALID_CREDENTIALS,
                    "Wallet does not belong to tenant");
        }

        LocalDate today = LocalDate.now(zone);
        Instant startOfDay = today.atStartOfDay(zone).toInstant();
        Instant startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1L)
                .atStartOfDay(zone).toInstant();
        Instant startOfMonth = today.withDayOfMonth(1).atStartOfDay(zone).toInstant();
        Instant startOfYear = today.withDayOfYear(1).atStartOfDay(zone).toInstant();

        BigDecimal todaySpent = limitEnforcer.spentSince(wallet, startOfDay);
        BigDecimal weekSpent = limitEnforcer.spentSince(wallet, startOfWeek);
        BigDecimal monthSpent = limitEnforcer.spentSince(wallet, startOfMonth);
        BigDecimal yearSpent = limitEnforcer.spentSince(wallet, startOfYear);

        BigDecimal dailyLimit = wallet.getDailyTransactionLimit();
        BigDecimal weeklyLimit = wallet.getWeeklyTransactionLimit();
        BigDecimal monthlyLimit = wallet.getMonthlyTransactionLimit();
        BigDecimal yearlyLimit = wallet.getYearlyTransactionLimit();

        WalletLimitBounds bounds = boundsRepository.findByTenantId(tenantId).orElse(null);

        return new WalletLimitView(
                walletId,
                wallet.getSingleTransactionLimit(),
                dailyLimit,
                weeklyLimit,
                monthlyLimit,
                yearlyLimit,
                todaySpent,
                weekSpent,
                monthSpent,
                yearSpent,
                bounds == null ? null : bounds.getMinSingleLimit(),
                bounds == null ? null : bounds.getMaxSingleLimit(),
                bounds == null ? null : bounds.getMinDailyLimit(),
                bounds == null ? null : bounds.getMaxDailyLimit(),
                bounds == null ? null : bounds.getMinWeeklyLimit(),
                bounds == null ? null : bounds.getMaxWeeklyLimit(),
                bounds == null ? null : bounds.getMinMonthlyLimit(),
                bounds == null ? null : bounds.getMaxMonthlyLimit(),
                bounds == null ? null : bounds.getMinYearlyLimit(),
                bounds == null ? null : bounds.getMaxYearlyLimit(),
                wallet.getCurrency());
    }
}
