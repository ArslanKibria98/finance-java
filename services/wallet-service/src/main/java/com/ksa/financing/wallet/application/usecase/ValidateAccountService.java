package com.ksa.financing.wallet.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.port.in.ValidateAccountUseCase;
import com.ksa.financing.wallet.domain.port.out.ScotiaEftPort;
import com.ksa.financing.wallet.domain.port.out.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Validates a destination account against our own wallets first, then (only on a hit)
 * via Scotia account-validation. transit is derived from the account number's first 5 digits.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValidateAccountService implements ValidateAccountUseCase {

    private final WalletRepository walletRepository;
    private final ScotiaEftPort scotiaEftPort;

    @Override
    @Transactional(readOnly = true)
    public ValidationResult validate(UUID tenantId, String accountNumber, String institutionNumber,
                                     String fullName, String currency) {
        String acct = accountNumber == null ? "" : accountNumber.trim();
        if (acct.isBlank()) {
            throw new BusinessException("ACCOUNT.VALIDATION.INVALID", "accountNumber is required");
        }
        if (institutionNumber == null || institutionNumber.isBlank()) {
            throw new BusinessException("ACCOUNT.VALIDATION.INVALID", "institutionNumber is required");
        }

        // 1) Must exist in our own wallets (exact account_number match) — else no match found.
        Optional<Wallet> wallet = walletRepository.findByAccountNumber(tenantId, acct);
        if (wallet.isEmpty()) {
            log.info("Account validation: no wallet match for account={}", acct);
            return new ValidationResult(false, "NO_MATCH_FOUND", acct, null, institutionNumber,
                    "No account found in our system for account number " + acct, null, null);
        }

        // 2) Derive transit = first 5 digits of the account number (non-digits ignored).
        String digits = acct.replaceAll("\\D", "");
        if (digits.length() < 5) {
            throw new BusinessException("ACCOUNT.VALIDATION.INVALID",
                    "accountNumber must contain at least 5 digits to derive transit");
        }
        String transit = digits.substring(0, 5);

        // 3) Scotia account-validation (fullName optional → may be null).
        var v = scotiaEftPort.validateAccount(institutionNumber, transit, acct, fullName);
        String status = v.valid() ? "VALID" : "INVALID";
        String message = v.valid()
                ? "Account validated"
                : "Account exists in our system but Scotia validation was not confirmed";
        log.info("Account validation: account={} transit={} valid={}", acct, transit, v.valid());
        return new ValidationResult(v.valid(), status, acct, transit, institutionNumber,
                message, v.ref(), v.raw());
    }
}
