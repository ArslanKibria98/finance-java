package com.ksa.financing.piivault.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity mapping to the {@code pii_individuals} table.
 * <p>
 * ALL PII fields are stored as BYTEA (byte[]) — encrypted at the adapter layer
 * using AES-256-GCM. The domain model stores plaintext strings; encryption and
 * decryption happen in the repository implementation via {@code EncryptionPort}.
 * <p>
 * Non-PII columns like {@code vault_region}, {@code country_code}, {@code gender},
 * {@code nationality_code}, and {@code national_id_type} are stored in plaintext
 * as they are needed for queries and do not constitute PII by themselves.
 */
@Entity
@Table(name = "pii_individuals")
@Getter
@Setter
public class PiiIndividualJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "pii_id", updatable = false, nullable = false)
    private UUID piiId;

    @Column(name = "global_uid", nullable = false)
    private UUID globalUid;

    @Enumerated(EnumType.STRING)
    @Column(name = "vault_region", nullable = false, columnDefinition = "vault_region")
    private VaultRegionEnum vaultRegion;

    // --- Encrypted PII fields (BYTEA) ---

    @Column(name = "national_id_encrypted", nullable = false, columnDefinition = "BYTEA")
    private byte[] nationalIdEncrypted;

    @Column(name = "national_id_type", nullable = false, length = 20)
    private String nationalIdType;

    @Column(name = "full_name_encrypted", nullable = false, columnDefinition = "BYTEA")
    private byte[] fullNameEncrypted;

    @Column(name = "first_name_encrypted", nullable = false, columnDefinition = "BYTEA")
    private byte[] firstNameEncrypted;

    @Column(name = "middle_name_encrypted", columnDefinition = "BYTEA")
    private byte[] middleNameEncrypted;

    @Column(name = "last_name_encrypted", nullable = false, columnDefinition = "BYTEA")
    private byte[] lastNameEncrypted;

    @Column(name = "full_name_ar_encrypted", columnDefinition = "BYTEA")
    private byte[] fullNameArEncrypted;

    @Column(name = "date_of_birth_encrypted", nullable = false, columnDefinition = "BYTEA")
    private byte[] dateOfBirthEncrypted;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "nationality_code", nullable = false, length = 3)
    private String nationalityCode;

    @Column(name = "mobile_encrypted", nullable = false, columnDefinition = "BYTEA")
    private byte[] mobileEncrypted;

    @Column(name = "email_encrypted", columnDefinition = "BYTEA")
    private byte[] emailEncrypted;

    @Column(name = "alternate_mobile_encrypted", columnDefinition = "BYTEA")
    private byte[] alternateMobileEncrypted;

    @Column(name = "address_line1_encrypted", columnDefinition = "BYTEA")
    private byte[] addressLine1Encrypted;

    @Column(name = "address_line2_encrypted", columnDefinition = "BYTEA")
    private byte[] addressLine2Encrypted;

    @Column(name = "city_encrypted", columnDefinition = "BYTEA")
    private byte[] cityEncrypted;

    @Column(name = "region_encrypted", columnDefinition = "BYTEA")
    private byte[] regionEncrypted;

    @Column(name = "postal_code_encrypted", columnDefinition = "BYTEA")
    private byte[] postalCodeEncrypted;

    @Column(name = "country_code", nullable = false, length = 3)
    private String countryCode;

    @Column(name = "national_address_encrypted", columnDefinition = "BYTEA")
    private byte[] nationalAddressEncrypted;

    @Column(name = "building_number_encrypted", columnDefinition = "BYTEA")
    private byte[] buildingNumberEncrypted;

    @Column(name = "unit_number_encrypted", columnDefinition = "BYTEA")
    private byte[] unitNumberEncrypted;

    @Column(name = "employer_name_encrypted", columnDefinition = "BYTEA")
    private byte[] employerNameEncrypted;

    @Column(name = "employer_cr_encrypted", columnDefinition = "BYTEA")
    private byte[] employerCrEncrypted;

    @Column(name = "monthly_salary_encrypted", columnDefinition = "BYTEA")
    private byte[] monthlySalaryEncrypted;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "iban_encrypted", columnDefinition = "BYTEA")
    private byte[] ibanEncrypted;

    @Column(name = "account_holder_name_encrypted", columnDefinition = "BYTEA")
    private byte[] accountHolderNameEncrypted;

    // --- Encryption metadata ---

    @Column(name = "encryption_key_version", nullable = false)
    private int encryptionKeyVersion;

    @Column(name = "encryption_algorithm", nullable = false, length = 50)
    private String encryptionAlgorithm;

    @Column(name = "encrypted_at", nullable = false)
    private OffsetDateTime encryptedAt;

    // --- Timestamps and version ---

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "deletion_reason", length = 500)
    private String deletionReason;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    /**
     * Enum for vault_region PostgreSQL enum.
     */
    public enum VaultRegionEnum {
        KSA,
        UAE,
        PAK,
        EGY,
        MYS
    }
}
