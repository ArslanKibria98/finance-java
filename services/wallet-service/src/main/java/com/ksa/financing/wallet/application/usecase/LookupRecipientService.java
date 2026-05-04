package com.ksa.financing.wallet.application.usecase;

import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.model.WalletStatus;
import com.ksa.financing.wallet.domain.port.in.LookupRecipientUseCase;
import com.ksa.financing.wallet.domain.port.out.RecipientLookupPort;
import com.ksa.financing.wallet.domain.port.out.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LookupRecipientService implements LookupRecipientUseCase {

    private final RecipientLookupPort recipientLookupPort;
    private final WalletRepository walletRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<RecipientView> lookup(String mobileNumber) {
        if (mobileNumber == null || mobileNumber.isBlank()) {
            return Optional.empty();
        }

        var idLookup = recipientLookupPort.lookupByMobile(mobileNumber.trim());
        if (idLookup.isEmpty() || idLookup.get().customerId() == null) {
            log.info("Recipient lookup miss for mobile=****{}",
                    mobileNumber.length() >= 4 ? mobileNumber.substring(mobileNumber.length() - 4) : "");
            return Optional.empty();
        }

        var info = idLookup.get();
        var wallet = walletRepository.findByCustomerId(info.tenantId(), info.customerId());
        if (wallet.isEmpty()) {
            return Optional.of(new RecipientView(
                    null, null,
                    maskName(info.firstName(), info.lastName(), info.name()),
                    info.maskedMobile(),
                    null,
                    null,
                    info.status(),
                    false,
                    "RECIPIENT_HAS_NO_WALLET"));
        }

        Wallet w = wallet.get();
        boolean canReceive = w.getStatus() == WalletStatus.ACTIVE && info.enabled();
        String reason = canReceive ? null
                : (w.getStatus() != WalletStatus.ACTIVE
                        ? "WALLET_NOT_ACTIVE_" + w.getStatus().name()
                        : "USER_DISABLED");

        return Optional.of(new RecipientView(
                w.getId(),
                w.getWalletNumber(),
                maskName(info.firstName(), info.lastName(), info.name()),
                info.maskedMobile(),
                w.getCurrency(),
                w.getStatus(),
                info.status(),
                canReceive,
                reason));
    }

    private String maskName(String firstName, String lastName, String fullName) {
        if (firstName != null && !firstName.isBlank()) {
            String first = firstName.trim();
            String last = lastName != null && !lastName.isBlank() ? lastName.trim() : "";
            String firstShown = first.length() <= 1 ? first : first.charAt(0) + "***";
            String lastShown  = last.isEmpty() ? "" : (last.length() <= 1 ? last : " " + last.charAt(0) + "***");
            return firstShown + lastShown;
        }
        if (fullName != null && !fullName.isBlank()) {
            return fullName.charAt(0) + "*** " + (fullName.length() > 2 ? fullName.charAt(fullName.length() - 1) + "***" : "***");
        }
        return "Unknown";
    }
}
