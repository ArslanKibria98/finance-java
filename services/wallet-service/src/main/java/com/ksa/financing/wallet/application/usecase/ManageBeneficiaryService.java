package com.ksa.financing.wallet.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.wallet.domain.model.IbanBeneficiary;
import com.ksa.financing.wallet.domain.port.in.ManageBeneficiaryUseCase;
import com.ksa.financing.wallet.domain.port.out.IbanBeneficiaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManageBeneficiaryService implements ManageBeneficiaryUseCase {

    private static final Pattern IBAN_REGEX = Pattern.compile("^[A-Z]{2}[0-9]{2}[A-Z0-9]{1,30}$");

    private final IbanBeneficiaryRepository repository;

    @Override
    @Transactional
    public IbanBeneficiary add(AddBeneficiaryCommand cmd) {
        String normalizedIban = cmd.iban() == null ? null
                : cmd.iban().replaceAll("\\s+", "").toUpperCase();
        if (normalizedIban == null || !IBAN_REGEX.matcher(normalizedIban).matches()) {
            throw new BusinessException("WALLET.BENEFICIARY.IBAN_INVALID",
                    "IBAN format is invalid");
        }

        var existing = repository.findByCustomerAndIban(cmd.tenantId(), cmd.customerId(), normalizedIban);
        if (existing.isPresent()) {
            log.info("Beneficiary already exists customerId={} iban={}",
                    cmd.customerId(), maskIban(normalizedIban));
            return existing.get();
        }

        IbanBeneficiary b = new IbanBeneficiary();
        b.setId(UUID.randomUUID());
        b.setTenantId(cmd.tenantId());
        b.setCustomerId(cmd.customerId());
        b.setWalletId(cmd.walletId());
        b.setNickname(cmd.nickname());
        b.setBeneficiaryName(cmd.beneficiaryName());
        b.setIban(normalizedIban);
        b.setBankCode(cmd.bankCode());
        b.setBankName(cmd.bankName());
        b.setVerified(false);
        b.setActive(true);
        b.setCreatedAt(Instant.now());
        b.setUpdatedAt(Instant.now());
        return repository.save(b);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IbanBeneficiary> listByCustomer(UUID tenantId, UUID customerId) {
        return repository.findActiveByCustomer(tenantId, customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public IbanBeneficiary getById(UUID tenantId, UUID beneficiaryId) {
        return repository.findByIdAndTenantId(beneficiaryId, tenantId)
                .orElseThrow(() -> NotFoundException.forEntity("Beneficiary", beneficiaryId.toString()));
    }

    @Override
    @Transactional
    public void deactivate(UUID tenantId, UUID beneficiaryId) {
        IbanBeneficiary b = getById(tenantId, beneficiaryId);
        b.setActive(false);
        b.setUpdatedAt(Instant.now());
        repository.save(b);
    }

    @Override
    @Transactional
    public IbanBeneficiary activate(UUID tenantId, UUID beneficiaryId) {
        IbanBeneficiary b = getById(tenantId, beneficiaryId);
        if (b.isActive()) {
            log.info("Beneficiary already active id={}", beneficiaryId);
            return b;
        }
        b.setActive(true);
        b.setUpdatedAt(Instant.now());
        return repository.save(b);
    }

    private String maskIban(String iban) {
        if (iban == null || iban.length() < 8) return "****";
        return iban.substring(0, 4) + "..." + iban.substring(iban.length() - 4);
    }
}
