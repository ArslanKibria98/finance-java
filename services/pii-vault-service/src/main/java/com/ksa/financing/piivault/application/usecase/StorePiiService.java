package com.ksa.financing.piivault.application.usecase;

import com.ksa.financing.piivault.domain.model.PiiIndividual;
import com.ksa.financing.piivault.domain.model.VaultRegion;
import com.ksa.financing.piivault.domain.port.in.StorePiiUseCase;
import com.ksa.financing.piivault.domain.port.out.PiiRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class StorePiiService implements StorePiiUseCase {

    private final PiiRepository piiRepository;

    public StorePiiService(PiiRepository piiRepository) {
        this.piiRepository = piiRepository;
    }

    @Override
    @Transactional
    public PiiIndividual store(StorePiiCommand command) {
        var existing = piiRepository.findByGlobalUid(command.globalUid());
        if (existing.isPresent()) {
            return existing.get();
        }

        Instant now = Instant.now();

        PiiIndividual pii = new PiiIndividual();
        pii.setGlobalUid(command.globalUid());
        pii.setVaultRegion(VaultRegion.KSA);
        pii.setNationalId(command.nationalId());
        pii.setNationalIdType(command.nationalIdType());
        pii.setFullName(command.fullName());
        pii.setFirstName(command.firstName());
        pii.setMiddleName(command.middleName());
        pii.setLastName(command.lastName());
        pii.setFullNameAr(command.fullNameAr());
        pii.setDateOfBirth(command.dateOfBirth());
        pii.setGender(command.gender());
        pii.setNationalityCode(command.nationalityCode());
        pii.setMobile(command.mobile());
        pii.setEmail(command.email());
        pii.setCountryCode(command.countryCode());
        pii.setEncryptionKeyVersion(1);
        pii.setCreatedAt(now);
        pii.setUpdatedAt(now);

        return piiRepository.save(pii);
    }
}
