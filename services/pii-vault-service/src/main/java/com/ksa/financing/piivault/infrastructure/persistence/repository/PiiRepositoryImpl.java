package com.ksa.financing.piivault.infrastructure.persistence.repository;

import com.ksa.financing.piivault.domain.model.PiiIndividual;
import com.ksa.financing.piivault.domain.port.out.EncryptionPort;
import com.ksa.financing.piivault.domain.port.out.PiiRepository;
import com.ksa.financing.piivault.infrastructure.persistence.entity.PiiIndividualJpaEntity;
import com.ksa.financing.piivault.infrastructure.persistence.mapper.PiiPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence adapter implementing {@link PiiRepository}.
 * <p>
 * This adapter is responsible for encrypting PII fields before persistence
 * and decrypting them after retrieval. The mapper handles non-encrypted
 * field mapping; this class bridges the gap for encrypted BYTEA columns.
 * <p>
 * Encryption flow:
 * <ol>
 *   <li>Domain model has plaintext strings</li>
 *   <li>Mapper converts non-PII fields to entity</li>
 *   <li>This adapter encrypts each PII string → byte[] and sets on entity</li>
 *   <li>JPA persists entity with BYTEA columns</li>
 * </ol>
 * Decryption flow is the reverse.
 */
@Component
@RequiredArgsConstructor
public class PiiRepositoryImpl implements PiiRepository {

    private final JpaPiiIndividualRepository jpaRepository;
    private final PiiPersistenceMapper mapper;
    private final EncryptionPort encryptionPort;

    @Override
    public PiiIndividual save(PiiIndividual piiIndividual) {
        PiiIndividualJpaEntity entity = mapper.toEntity(piiIndividual);

        // Encrypt all PII fields from domain plaintext to entity BYTEA
        encryptFields(entity, piiIndividual);
        entity.setEncryptionKeyVersion(encryptionPort.getCurrentKeyVersion());

        PiiIndividualJpaEntity saved = jpaRepository.save(entity);

        // Map back to domain and decrypt
        return toDomainWithDecryption(saved);
    }

    @Override
    public Optional<PiiIndividual> findByGlobalUid(UUID globalUid) {
        return jpaRepository.findByGlobalUidAndDeletedAtIsNull(globalUid)
                .map(this::toDomainWithDecryption);
    }

    @Override
    public void deleteByGlobalUid(UUID globalUid) {
        jpaRepository.findByGlobalUidAndDeletedAtIsNull(globalUid)
                .ifPresent(entity -> {
                    // Soft delete — set deleted_at and null out encrypted data
                    entity.setDeletedAt(java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC));
                    entity.setNationalIdEncrypted(new byte[0]);
                    entity.setFullNameEncrypted(new byte[0]);
                    entity.setFirstNameEncrypted(new byte[0]);
                    entity.setMiddleNameEncrypted(null);
                    entity.setLastNameEncrypted(new byte[0]);
                    entity.setFullNameArEncrypted(null);
                    entity.setDateOfBirthEncrypted(new byte[0]);
                    entity.setMobileEncrypted(new byte[0]);
                    entity.setEmailEncrypted(null);
                    entity.setAlternateMobileEncrypted(null);
                    entity.setAddressLine1Encrypted(null);
                    entity.setAddressLine2Encrypted(null);
                    entity.setCityEncrypted(null);
                    entity.setRegionEncrypted(null);
                    entity.setPostalCodeEncrypted(null);
                    entity.setNationalAddressEncrypted(null);
                    entity.setBuildingNumberEncrypted(null);
                    entity.setUnitNumberEncrypted(null);
                    entity.setEmployerNameEncrypted(null);
                    entity.setEmployerCrEncrypted(null);
                    entity.setMonthlySalaryEncrypted(null);
                    entity.setIbanEncrypted(null);
                    entity.setAccountHolderNameEncrypted(null);
                    jpaRepository.save(entity);
                });
    }

    // -----------------------------------------------------------------------
    // Encryption helpers
    // -----------------------------------------------------------------------

    /**
     * Encrypts all PII fields from the domain model and sets them on the entity.
     */
    private void encryptFields(PiiIndividualJpaEntity entity, PiiIndividual domain) {
        entity.setNationalIdEncrypted(encryptNullable(domain.getNationalId()));
        entity.setFullNameEncrypted(encryptNullable(domain.getFullName()));
        entity.setFirstNameEncrypted(encryptNullable(domain.getFirstName()));
        entity.setMiddleNameEncrypted(encryptNullable(domain.getMiddleName()));
        entity.setLastNameEncrypted(encryptNullable(domain.getLastName()));
        entity.setFullNameArEncrypted(encryptNullable(domain.getFullNameAr()));
        entity.setDateOfBirthEncrypted(encryptNullable(domain.getDateOfBirth()));
        entity.setMobileEncrypted(encryptNullable(domain.getMobile()));
        entity.setEmailEncrypted(encryptNullable(domain.getEmail()));
        entity.setAlternateMobileEncrypted(encryptNullable(domain.getAlternateMobile()));
        entity.setAddressLine1Encrypted(encryptNullable(domain.getAddressLine1()));
        entity.setAddressLine2Encrypted(encryptNullable(domain.getAddressLine2()));
        entity.setCityEncrypted(encryptNullable(domain.getCity()));
        entity.setRegionEncrypted(encryptNullable(domain.getRegion()));
        entity.setPostalCodeEncrypted(encryptNullable(domain.getPostalCode()));
        entity.setEmployerNameEncrypted(encryptNullable(domain.getEmployerName()));
        entity.setEmployerCrEncrypted(encryptNullable(domain.getEmployerCr()));
        entity.setMonthlySalaryEncrypted(encryptNullable(domain.getMonthlySalary()));
        entity.setIbanEncrypted(encryptNullable(domain.getIban()));
        entity.setAccountHolderNameEncrypted(encryptNullable(domain.getAccountHolderName()));
    }

    /**
     * Decrypts all PII fields from the entity and returns a complete domain model.
     */
    private PiiIndividual toDomainWithDecryption(PiiIndividualJpaEntity entity) {
        PiiIndividual domain = mapper.toDomain(entity);

        // Decrypt all PII BYTEA fields to plaintext strings
        domain.setNationalId(decryptNullable(entity.getNationalIdEncrypted()));
        domain.setFullName(decryptNullable(entity.getFullNameEncrypted()));
        domain.setFirstName(decryptNullable(entity.getFirstNameEncrypted()));
        domain.setMiddleName(decryptNullable(entity.getMiddleNameEncrypted()));
        domain.setLastName(decryptNullable(entity.getLastNameEncrypted()));
        domain.setFullNameAr(decryptNullable(entity.getFullNameArEncrypted()));
        domain.setDateOfBirth(decryptNullable(entity.getDateOfBirthEncrypted()));
        domain.setMobile(decryptNullable(entity.getMobileEncrypted()));
        domain.setEmail(decryptNullable(entity.getEmailEncrypted()));
        domain.setAlternateMobile(decryptNullable(entity.getAlternateMobileEncrypted()));
        domain.setAddressLine1(decryptNullable(entity.getAddressLine1Encrypted()));
        domain.setAddressLine2(decryptNullable(entity.getAddressLine2Encrypted()));
        domain.setCity(decryptNullable(entity.getCityEncrypted()));
        domain.setRegion(decryptNullable(entity.getRegionEncrypted()));
        domain.setPostalCode(decryptNullable(entity.getPostalCodeEncrypted()));
        domain.setEmployerName(decryptNullable(entity.getEmployerNameEncrypted()));
        domain.setEmployerCr(decryptNullable(entity.getEmployerCrEncrypted()));
        domain.setMonthlySalary(decryptNullable(entity.getMonthlySalaryEncrypted()));
        domain.setIban(decryptNullable(entity.getIbanEncrypted()));
        domain.setAccountHolderName(decryptNullable(entity.getAccountHolderNameEncrypted()));

        return domain;
    }

    private byte[] encryptNullable(String plaintext) {
        if (plaintext == null) return null;
        return encryptionPort.encrypt(plaintext);
    }

    private String decryptNullable(byte[] ciphertext) {
        if (ciphertext == null || ciphertext.length == 0) return null;
        return encryptionPort.decrypt(ciphertext);
    }
}
