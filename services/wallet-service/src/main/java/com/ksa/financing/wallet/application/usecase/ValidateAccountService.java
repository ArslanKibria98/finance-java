package com.ksa.financing.wallet.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.wallet.application.support.RecipientAccountResolver;
import com.ksa.financing.wallet.domain.port.in.ValidateAccountUseCase;
import com.ksa.financing.wallet.domain.port.out.ScotiaEftPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Validates a destination account via Scotia account-validation. Shared by two flows:
 *  - FT  (type=phone):   accountNumber carries the recipient's MOBILE number → verify the customer
 *                        by mobile → take their wallet account number forward to Scotia.
 *  - IBFT (type=account): accountNumber is a real bank account number → sent straight to Scotia as-is.
 * transit is derived from the first 5 digits of the resolved account number.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ValidateAccountService implements ValidateAccountUseCase {

    private final ScotiaEftPort scotiaEftPort;
    private final RecipientAccountResolver recipientAccountResolver;

    @Override
    @Transactional(readOnly = true)
    public ValidationResult validate(UUID tenantId, String accountNumber, String institutionNumber,
                                     String fullName, String currency, String type) {
        String raw = accountNumber == null ? "" : accountNumber.trim();
        if (raw.isBlank()) {
            throw new BusinessException("ACCOUNT.VALIDATION.INVALID", "accountNumber is required");
        }
        if (institutionNumber == null || institutionNumber.isBlank()) {
            throw new BusinessException("ACCOUNT.VALIDATION.INVALID", "institutionNumber is required");
        }

        if (isPhone(type, raw)) {
            // FT: accountNumber carries the recipient's MOBILE number — verify the customer and
            // take their wallet account number forward. Rejects if no matching customer/wallet.
            var resolved = recipientAccountResolver.resolveMobile(tenantId, raw);
            String name = (fullName == null || fullName.isBlank()) ? resolved.name() : fullName;
            log.info("Account validation (FT/phone): mobile={} → account={}", raw, resolved.accountNumber());
            return scotiaValidate(institutionNumber, resolved.accountNumber(), name);
        }

        // IBFT: accountNumber is a real bank account number — send straight to Scotia as-is.
        log.info("Account validation (IBFT/account): account={}", raw);
        return scotiaValidate(institutionNumber, raw, fullName);
    }

    /**
     * Decide whether the supplied value should be treated as a phone number (FT) or a direct
     * account number (IBFT). Explicit {@code type} wins; otherwise auto-detect from the format.
     */
    private boolean isPhone(String type, String raw) {
        if (type != null && !type.isBlank()) {
            String t = type.trim().toUpperCase();
            if (t.equals("PHONE") || t.equals("MOBILE") || t.equals("FT")) {
                return true;
            }
            if (t.equals("ACCOUNT") || t.equals("ACCOUNT_NUMBER") || t.equals("IBFT")) {
                return false;
            }
        }
        // No (or unrecognised) type → account-number format means a direct account, else a phone.
        return !RecipientAccountResolver.looksLikeAccountNumber(raw);
    }

    private ValidationResult scotiaValidate(String institutionNumber, String acct, String fullName) {
        // transit = first 5 digits of the account number (non-digits ignored).
        String digits = acct.replaceAll("\\D", "");
        if (digits.length() < 5) {
            throw new BusinessException("ACCOUNT.VALIDATION.INVALID",
                    "accountNumber must contain at least 5 digits to derive transit");
        }
        String transit = digits.substring(0, 5);

        var v = scotiaEftPort.validateAccount(institutionNumber, transit, acct, fullName);
        String status = v.valid() ? "VALID" : "INVALID";
        String message = v.valid() ? "Account validated" : "Scotia validation was not confirmed";
        log.info("Account validation: account={} transit={} valid={}", acct, transit, v.valid());
        return new ValidationResult(v.valid(), status, acct, transit, institutionNumber,
                message, v.ref(), v.raw());
    }
}
