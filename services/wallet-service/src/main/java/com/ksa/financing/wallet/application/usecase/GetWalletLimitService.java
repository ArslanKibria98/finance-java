package com.ksa.financing.wallet.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.wallet.application.support.TransactionLimitEnforcer;
import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.port.in.GetWalletLimitUseCase;
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
    private final TransactionLimitEnforcer limitEnforcer;
    private final ZoneId zone;

    public GetWalletLimitService(
            WalletRepository walletRepository,
            TransactionLimitEnforcer limitEnforcer,
            @Value("${wallet.limits.timezone:Asia/Riyadh}") String timezone) {
        this.walletRepository = walletRepository;
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
        Instant startOfMonth = today.withDayOfMonth(1).atStartOfDay(zone).toInstant();
        Instant startOfYear = today.withDayOfYear(1).atStartOfDay(zone).toInstant();

        BigDecimal todaySpent = limitEnforcer.spentSince(wallet, startOfDay);
        BigDecimal monthSpent = limitEnforcer.spentSince(wallet, startOfMonth);
        BigDecimal yearSpent = limitEnforcer.spentSince(wallet, startOfYear);

        BigDecimal dailyLimit = wallet.getDailyTransactionLimit();
        BigDecimal monthlyLimit = wallet.getMonthlyTransactionLimit();
        BigDecimal yearlyLimit = wallet.getYearlyTransactionLimit();

        return new WalletLimitView(
                walletId,
                dailyLimit,
                monthlyLimit,
                yearlyLimit,
                todaySpent,
                monthSpent,
                yearSpent,
                remaining(dailyLimit, todaySpent),
                remaining(monthlyLimit, monthSpent),
                remaining(yearlyLimit, yearSpent),
                wallet.getCurrency());
    }

    private BigDecimal remaining(BigDecimal limit, BigDecimal spent) {
        if (limit == null) return null;
        BigDecimal r = limit.subtract(spent);
        return r.signum() < 0 ? BigDecimal.ZERO : r;
    }
}
