package com.ksa.financing.wallet.application.support;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.port.out.RecipientLookupPort;
import com.ksa.financing.wallet.domain.port.out.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Resolves the recipient identifier supplied on an external-transfer / account-validation
 * request into the recipient's virtual wallet account number.
 *
 * Callers now send the recipient's MOBILE number in the account field
 * (accountNumber / creditorAccount / depositHandle). This component looks the customer up
 * by mobile (identity-service) → wallet → {@code account_number} and substitutes that
 * account number before the value is forwarded to Scotia.
 *
 * Backward compatible: if the value already looks like a wallet account number
 * (NNN-NNNNN-NNNNNNN), it is passed through unchanged.
 *
 * If the value is a mobile number with no matching customer / wallet / account number,
 * the resolution is REJECTED (BusinessException) and Scotia is never called.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecipientAccountResolver {

    /** Wallet virtual account numbers look like {@code 002-80150-0000097} (three dash-separated digit groups). */
    private static final Pattern ACCOUNT_NUMBER = Pattern.compile("^\\d{3}-\\d{4,6}-\\d{5,9}$");

    private final RecipientLookupPort recipientLookupPort;
    private final WalletRepository walletRepository;

    /** Resolution outcome — the destination account number plus the recipient's display name (may be null). */
    public record Resolved(String accountNumber, String name) {}

    /** True when the value already looks like a wallet/bank account number ({@code NNN-NNNNN-NNNNNNN}). */
    public static boolean looksLikeAccountNumber(String value) {
        return value != null && ACCOUNT_NUMBER.matcher(value.trim()).matches();
    }

    /**
     * Auto-detecting resolution: account-number format → passed through unchanged; otherwise the
     * value is treated as a mobile number and resolved to the recipient's wallet account number.
     *
     * @param tenantId   tenant the transfer belongs to (recipient wallet is looked up within it)
     * @param identifier recipient mobile number (or, for backward compatibility, an account number)
     */
    public Resolved resolve(UUID tenantId, String identifier) {
        String value = identifier == null ? "" : identifier.trim();
        if (value.isBlank()) {
            throw new BusinessException("WALLET.RECIPIENT.REQUIRED",
                    "Recipient mobile number is required");
        }

        // Already an account number → pass through unchanged (legacy / IBFT callers).
        if (looksLikeAccountNumber(value)) {
            return new Resolved(value, null);
        }
        return resolveMobile(tenantId, value);
    }

    /**
     * Strict mobile resolution: looks the customer up by mobile → wallet → {@code account_number}.
     * Rejects (BusinessException) if there is no matching customer / wallet / account number.
     */
    public Resolved resolveMobile(UUID tenantId, String mobileNumber) {
        String value = mobileNumber == null ? "" : mobileNumber.trim();
        if (value.isBlank()) {
            throw new BusinessException("WALLET.RECIPIENT.REQUIRED",
                    "Recipient mobile number is required");
        }

        var user = recipientLookupPort.lookupByMobile(value)
                .orElseThrow(() -> new BusinessException("WALLET.RECIPIENT.NOT_FOUND",
                        "No customer found for mobile number " + value));
        if (user.customerId() == null) {
            throw new BusinessException("WALLET.RECIPIENT.NOT_FOUND",
                    "No customer profile for mobile number " + value);
        }

        Wallet wallet = walletRepository.findByCustomerId(tenantId, user.customerId())
                .orElseThrow(() -> new BusinessException("WALLET.RECIPIENT.NO_WALLET",
                        "Recipient has no wallet for mobile number " + value));
        if (wallet.getAccountNumber() == null || wallet.getAccountNumber().isBlank()) {
            throw new BusinessException("WALLET.RECIPIENT.NO_ACCOUNT",
                    "Recipient wallet has no account number");
        }

        log.info("Resolved recipient mobile → account: mobile={} customer={} account={}",
                value, user.customerId(), wallet.getAccountNumber());
        return new Resolved(wallet.getAccountNumber(), user.name());
    }
}
