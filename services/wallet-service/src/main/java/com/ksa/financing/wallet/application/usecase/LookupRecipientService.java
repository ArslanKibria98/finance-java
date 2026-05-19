package com.ksa.financing.wallet.application.usecase;

import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.model.WalletStatus;
import com.ksa.financing.wallet.domain.port.in.LookupRecipientUseCase;
import com.ksa.financing.wallet.domain.port.out.RecipientLookupPort;
import com.ksa.financing.wallet.domain.port.out.RecipientLookupPort.UserLookup;
import com.ksa.financing.wallet.domain.port.out.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LookupRecipientService implements LookupRecipientUseCase {

    private final RecipientLookupPort recipientLookupPort;
    private final WalletRepository walletRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<RecipientView> lookup(UUID tenantId, String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            return Optional.empty();
        }
        String trimmed = accountNumber.trim();
        return looksLikeIban(trimmed) ? lookupByIban(tenantId, trimmed) : lookupByMobile(trimmed);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecipientCheck> checkRecipientsByMobile(List<String> mobileNumbers) {
        if (mobileNumbers == null || mobileNumbers.isEmpty()) {
            return List.of();
        }
        List<RecipientCheck> results = new ArrayList<>(mobileNumbers.size());
        for (String raw : mobileNumbers) {
            if (raw == null || raw.isBlank()) {
                results.add(notFoundCheck());
                continue;
            }
            String mobile = raw.trim();
            Optional<RecipientView> view = lookupByMobile(mobile);
            results.add(view.map(this::toCheck).orElseGet(this::notFoundCheck));
        }
        return results;
    }

    private RecipientCheck toCheck(RecipientView v) {
        return new RecipientCheck(
                true,
                v.walletId(),
                v.walletNumber(),
                v.iban(),
                v.maskedName(),
                v.maskedMobile(),
                v.currency(),
                v.walletStatus(),
                v.userStatus(),
                v.canReceive(),
                v.reason());
    }

    private RecipientCheck notFoundCheck() {
        return new RecipientCheck(
                false, null, null, null, null, null, null, null, null, false, "RECIPIENT_NOT_FOUND");
    }

    private Optional<RecipientView> lookupByMobile(String mobileNumber) {
        var idLookup = recipientLookupPort.lookupByMobile(mobileNumber);
        if (idLookup.isEmpty() || idLookup.get().customerId() == null) {
            log.info("Recipient lookup miss for mobile=****{}",
                    mobileNumber.length() >= 4 ? mobileNumber.substring(mobileNumber.length() - 4) : "");
            return Optional.empty();
        }
        UserLookup info = idLookup.get();
        var wallet = walletRepository.findByCustomerId(info.tenantId(), info.customerId());
        if (wallet.isEmpty()) {
            return Optional.of(emptyWalletView(info));
        }
        return Optional.of(buildView(wallet.get(), info));
    }

    private Optional<RecipientView> lookupByIban(UUID tenantId, String iban) {
        if (tenantId == null) {
            log.warn("IBAN lookup requested without tenantId");
            return Optional.empty();
        }
        var wallet = walletRepository.findByIban(tenantId, iban);
        if (wallet.isEmpty()) {
            log.info("Recipient lookup miss for iban=****{}",
                    iban.length() >= 4 ? iban.substring(iban.length() - 4) : "");
            return Optional.empty();
        }
        Wallet w = wallet.get();
        UserLookup info = recipientLookupPort.lookupByCustomerId(w.getCustomerId()).orElse(null);
        return Optional.of(buildView(w, info));
    }

    private RecipientView buildView(Wallet w, UserLookup info) {
        boolean userEnabled = info == null || info.enabled();
        boolean canReceive = w.getStatus() == WalletStatus.ACTIVE && userEnabled;
        String reason = canReceive ? null
                : (w.getStatus() != WalletStatus.ACTIVE
                        ? "WALLET_NOT_ACTIVE_" + w.getStatus().name()
                        : "USER_DISABLED");
        // Single source of truth = wallet.maskedName (frozen at creation from the
        // NAFATH pool name forwarded via customer-created event). No runtime
        // re-derivation, no random fallback.
        String maskedName = w.getMaskedName();
        String maskedMobile = info != null ? info.maskedMobile() : null;
        String userStatus = info != null ? info.status() : null;
        return new RecipientView(
                w.getId(),
                w.getWalletNumber(),
                w.getIban(),
                maskedName,
                maskedMobile,
                w.getCurrency(),
                w.getStatus(),
                userStatus,
                canReceive,
                reason);
    }

    private RecipientView emptyWalletView(UserLookup info) {
        // No wallet exists yet → no stored maskedName. Pass through the NAFATH
        // name (from identity-service) as-is; this is still the pool name set on
        // the customer, never a random runtime value.
        return new RecipientView(
                null, null, null,
                resolveIdentityName(info),
                info.maskedMobile(),
                null,
                null,
                info.status(),
                false,
                "RECIPIENT_HAS_NO_WALLET");
    }

    private String resolveIdentityName(UserLookup info) {
        if (info == null) return null;
        if (info.name() != null && !info.name().isBlank()) return info.name().trim();
        String first = info.firstName() != null ? info.firstName().trim() : "";
        String last = info.lastName() != null ? info.lastName().trim() : "";
        String joined = (first + " " + last).trim();
        return joined.isEmpty() ? null : joined;
    }

    private boolean looksLikeIban(String value) {
        if (value.length() < 15 || value.length() > 34) return false;
        char c0 = value.charAt(0);
        char c1 = value.charAt(1);
        return Character.isLetter(c0) && Character.isLetter(c1);
    }
}
