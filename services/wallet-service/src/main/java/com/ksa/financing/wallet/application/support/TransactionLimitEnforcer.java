package com.ksa.financing.wallet.application.support;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.port.out.WalletMovementRepository;
import com.ksa.financing.wallet.domain.port.out.WalletWithdrawalRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Enforces per-wallet daily and monthly transaction (spend) limits before a debit is applied.
 * Cumulative spend is computed live from outgoing transfers ({@code wallet_movements} TRANSFER_OUT)
 * plus outbound withdrawals ({@code wallet_withdrawals}), so there is no counter to reset and no
 * drift. Calendar day/month boundaries use a configurable timezone (KSA default).
 */
@Slf4j
@Component
public class TransactionLimitEnforcer {

    private final WalletMovementRepository movementRepository;
    private final WalletWithdrawalRepository withdrawalRepository;
    private final ZoneId zone;

    public TransactionLimitEnforcer(
            WalletMovementRepository movementRepository,
            WalletWithdrawalRepository withdrawalRepository,
            @Value("${wallet.limits.timezone:Asia/Riyadh}") String timezone) {
        this.movementRepository = movementRepository;
        this.withdrawalRepository = withdrawalRepository;
        this.zone = ZoneId.of(timezone);
    }

    /** Total counted spend (transfers + withdrawals) for a wallet since the given instant. */
    public BigDecimal spentSince(Wallet wallet, Instant since) {
        BigDecimal transfers = movementRepository.sumSpendSince(wallet.getTenantId(), wallet.getId(), since);
        BigDecimal withdrawals = withdrawalRepository.sumWithdrawnSince(wallet.getTenantId(), wallet.getId(), since);
        return transfers.add(withdrawals);
    }

    /**
     * @throws BusinessException (WALLET.BALANCE.LIMIT_EXCEEDED) if this amount would breach the
     *         wallet's daily or monthly transaction limit.
     */
    public void enforce(Wallet wallet, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            return;
        }
        // Single (per-transaction) limit — compares THIS amount, not cumulative.
        BigDecimal singleLimit = wallet.getSingleTransactionLimit();
        if (singleLimit != null && amount.compareTo(singleLimit) > 0) {
            log.info("Single-transaction limit breach wallet={} limit={} attempted={}",
                    wallet.getId(), singleLimit, amount);
            throw new BusinessException(
                    ErrorCodes.Wallet.SINGLE_TXN_LIMIT_EXCEEDED,
                    "Single-transaction limit exceeded: limit=" + singleLimit + " attempted=" + amount,
                    singleLimit, amount);
        }

        LocalDate today = LocalDate.now(zone);
        Instant startOfDay = today.atStartOfDay(zone).toInstant();
        Instant startOfMonth = today.withDayOfMonth(1).atStartOfDay(zone).toInstant();
        Instant startOfYear = today.withDayOfYear(1).atStartOfDay(zone).toInstant();

        BigDecimal dailyLimit = wallet.getDailyTransactionLimit();
        if (dailyLimit != null) {
            BigDecimal todaySpent = spentSince(wallet, startOfDay);
            if (todaySpent.add(amount).compareTo(dailyLimit) > 0) {
                log.info("Daily limit breach wallet={} limit={} used={} attempted={}",
                        wallet.getId(), dailyLimit, todaySpent, amount);
                throw new BusinessException(
                        ErrorCodes.Wallet.DAILY_TXN_LIMIT_EXCEEDED,
                        "Daily transaction limit exceeded: limit=" + dailyLimit
                                + " alreadyUsed=" + todaySpent + " attempted=" + amount,
                        dailyLimit, todaySpent, amount);
            }
        }

        BigDecimal monthlyLimit = wallet.getMonthlyTransactionLimit();
        if (monthlyLimit != null) {
            BigDecimal monthSpent = spentSince(wallet, startOfMonth);
            if (monthSpent.add(amount).compareTo(monthlyLimit) > 0) {
                log.info("Monthly limit breach wallet={} limit={} used={} attempted={}",
                        wallet.getId(), monthlyLimit, monthSpent, amount);
                throw new BusinessException(
                        ErrorCodes.Wallet.MONTHLY_TXN_LIMIT_EXCEEDED,
                        "Monthly transaction limit exceeded: limit=" + monthlyLimit
                                + " alreadyUsed=" + monthSpent + " attempted=" + amount,
                        monthlyLimit, monthSpent, amount);
            }
        }

        BigDecimal yearlyLimit = wallet.getYearlyTransactionLimit();
        if (yearlyLimit != null) {
            BigDecimal yearSpent = spentSince(wallet, startOfYear);
            if (yearSpent.add(amount).compareTo(yearlyLimit) > 0) {
                log.info("Yearly limit breach wallet={} limit={} used={} attempted={}",
                        wallet.getId(), yearlyLimit, yearSpent, amount);
                throw new BusinessException(
                        ErrorCodes.Wallet.YEARLY_TXN_LIMIT_EXCEEDED,
                        "Yearly transaction limit exceeded: limit=" + yearlyLimit
                                + " alreadyUsed=" + yearSpent + " attempted=" + amount,
                        yearlyLimit, yearSpent, amount);
            }
        }
    }
}
