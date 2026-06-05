package com.ksa.financing.wallet.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.wallet.domain.model.IbftBeneficiary;
import com.ksa.financing.wallet.domain.port.in.ManageIbftBeneficiaryUseCase;
import com.ksa.financing.wallet.domain.port.out.IbftBeneficiaryRepository;
import com.ksa.financing.wallet.domain.port.out.ScotiaEftPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IbftBeneficiaryService implements ManageIbftBeneficiaryUseCase {

    private final IbftBeneficiaryRepository repository;
    private final ScotiaEftPort scotiaEftPort;

    @Value("${ksa.wallet.ibft.validate-beneficiary-on-add:true}")
    private boolean validateOnAdd;

    @Override
    @Transactional
    public IbftBeneficiary add(AddBeneficiaryCommand c) {
        if (c.beneficiaryName() == null || c.beneficiaryName().isBlank())
            throw new BusinessException("IBFT.BENEFICIARY.INVALID", "beneficiaryName is required");
        String acctDigits = c.accountNumber() == null ? "" : c.accountNumber().replaceAll("\\D", "");
        if (notDigits(c.institutionNumber(), 3, 4) || acctDigits.length() < 5 || acctDigits.length() > 20)
            throw new BusinessException("IBFT.BENEFICIARY.INVALID",
                    "Invalid Canadian account: institution(3-4 digits) + account(>=5 digits) required");

        // transit is NOT supplied by the caller — derived from the first 5 digits of the account number
        String transit = acctDigits.substring(0, 5);

        // Duplicate guard
        repository.findExisting(c.tenantId(), c.customerId(), c.institutionNumber(), transit, acctDigits)
                .ifPresent(b -> { throw new BusinessException("IBFT.BENEFICIARY.DUPLICATE",
                        "Beneficiary already exists: " + c.institutionNumber() + "-" + transit + "-" + acctDigits); });

        IbftBeneficiary b = new IbftBeneficiary();
        b.setId(UUID.randomUUID());
        b.setTenantId(c.tenantId());
        b.setCustomerId(c.customerId());
        b.setWalletId(c.walletId());
        b.setNickname(c.nickname());
        b.setBeneficiaryName(c.beneficiaryName().trim());
        b.setInstitutionNumber(c.institutionNumber());
        b.setTransit(transit);
        b.setAccountNumber(acctDigits);
        b.setBankName(c.bankName());
        b.setCurrency(c.currency() != null ? c.currency() : "CAD");
        b.setActive(true);
        b.setCreatedAt(Instant.now());

        // Optional Scotia account validation before saving
        if (validateOnAdd) {
            var v = scotiaEftPort.validateAccount(c.institutionNumber(), transit,
                    acctDigits, c.beneficiaryName());
            b.setValidated(v.valid());
            b.setValidationRef(v.ref());
            b.setValidationResult(v.raw());
            if (!v.valid()) {
                log.warn("Beneficiary Scotia validation not confirmed (saving as unvalidated): status={}", v.status());
            }
        }
        return repository.save(b);
    }

    @Override
    public List<IbftBeneficiary> list(UUID tenantId, UUID customerId) {
        return repository.findByCustomer(tenantId, customerId);
    }

    @Override
    public IbftBeneficiary get(UUID tenantId, UUID beneficiaryId) {
        return repository.findByIdAndTenantId(beneficiaryId, tenantId)
                .orElseThrow(() -> NotFoundException.forEntity("IbftBeneficiary", beneficiaryId.toString()));
    }

    @Override
    @Transactional
    public IbftBeneficiary deactivate(UUID tenantId, UUID beneficiaryId) {
        IbftBeneficiary b = get(tenantId, beneficiaryId);
        b.setActive(false);
        return repository.save(b);
    }

    private static boolean notDigits(String s, int min, int max) {
        return s == null || !s.matches("^[0-9]{" + min + "," + max + "}$");
    }
}
