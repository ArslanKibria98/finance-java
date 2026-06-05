package com.ksa.financing.customer.infrastructure.persistence.mapper;

import com.ksa.financing.customer.domain.model.BankAccount;
import com.ksa.financing.customer.domain.model.BankAccountStatus;
import com.ksa.financing.customer.domain.model.Customer;
import com.ksa.financing.customer.domain.model.CustomerType;
import com.ksa.financing.customer.domain.model.EmploymentInfo;
import com.ksa.financing.customer.domain.model.EmploymentType;
import com.ksa.financing.customer.domain.model.Gender;
import com.ksa.financing.customer.domain.model.KycStatus;
import com.ksa.financing.customer.domain.model.LifecycleStage;
import com.ksa.financing.customer.domain.model.ResidencyType;
import com.ksa.financing.customer.domain.model.RiskGrade;
import com.ksa.financing.customer.infrastructure.persistence.entity.BankAccountJpaEntity;
import com.ksa.financing.customer.infrastructure.persistence.entity.CustomerJpaEntity;
import com.ksa.financing.customer.infrastructure.persistence.entity.EmploymentInfoJpaEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class CustomerPersistenceMapper {

    // ---- Customer mapping ----

    public static CustomerJpaEntity toEntity(Customer domain) {
        if (domain == null) return null;

        CustomerJpaEntity entity = new CustomerJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setCifNumber(domain.getCifNumber());
        entity.setCustomerType(domain.getCustomerType() != null ? domain.getCustomerType().name() : null);
        entity.setNationalId(domain.getNationalId());
        entity.setNationalIdType(domain.getNationalIdType());
        entity.setTitle(domain.getTitle());
        entity.setFirstName(domain.getFirstName());
        entity.setMiddleName(domain.getMiddleName());
        entity.setLastName(domain.getLastName());
        entity.setFirstNameAr(domain.getFirstNameAr());
        entity.setLastNameAr(domain.getLastNameAr());
        entity.setFullName(domain.getFullName());
        entity.setDateOfBirth(domain.getDateOfBirth());
        entity.setGender(domain.getGender() != null ? domain.getGender().name() : null);
        entity.setNationality(domain.getNationality());
        entity.setResidencyType(domain.getResidencyType() != null ? domain.getResidencyType().name() : null);
        entity.setMobileNumber(domain.getMobileNumber());
        entity.setEmail(domain.getEmail());
        entity.setAddressLine1(domain.getAddressLine1());
        entity.setAddressLine2(domain.getAddressLine2());
        entity.setCity(domain.getCity());
        entity.setRegion(domain.getRegion());
        entity.setPostalCode(domain.getPostalCode());
        entity.setCountry(domain.getCountry());
        entity.setKycStatus(domain.getKycStatus() != null ? domain.getKycStatus().name() : null);
        entity.setKycVerifiedAt(toOffsetDateTime(domain.getKycVerifiedAt()));
        entity.setKycExpiryDate(domain.getKycExpiryDate());
        entity.setNafathVerified(domain.isNafathVerified());
        entity.setNafathTransactionId(domain.getNafathTransactionId());
        entity.setRiskGrade(domain.getRiskGrade() != null ? domain.getRiskGrade().name() : null);
        entity.setRiskGradeUpdatedAt(toOffsetDateTime(domain.getRiskGradeUpdatedAt()));
        entity.setPepFlag(domain.isPepFlag());
        entity.setSanctionsFlag(domain.isSanctionsFlag());
        entity.setPepStatus(domain.getPepStatus() != null ? domain.getPepStatus().name() : null);
        entity.setKeycloakUserId(domain.getKeycloakUserId());
        entity.setGlobalUid(domain.getGlobalUid());
        entity.setLifecycleStage(domain.getLifecycleStage() != null ? domain.getLifecycleStage().name() : null);
        entity.setLifecycleStageChangedAt(toOffsetDateTime(domain.getLifecycleStageChangedAt()));
        entity.setCustomerSegment(domain.getCustomerSegment());
        entity.setOnboardingFlow(domain.getOnboardingFlow());
        entity.setAcquisitionChannel(domain.getAcquisitionChannel());
        entity.setAcquisitionPartnerId(domain.getAcquisitionPartnerId());
        entity.setActive(domain.isActive());
        entity.setBlockedAt(toOffsetDateTime(domain.getBlockedAt()));
        entity.setBlockedReason(domain.getBlockedReason());
        entity.setProfilePicture(domain.getProfilePicture());
        entity.setIdempotencyKey(domain.getIdempotencyKey());
        entity.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
        entity.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));
        entity.setVersion(domain.getVersion());
        return entity;
    }

    public static Customer toDomain(CustomerJpaEntity entity) {
        if (entity == null) return null;

        Customer domain = new Customer();
        domain.setId(entity.getId());
        domain.setTenantId(entity.getTenantId());
        domain.setCifNumber(entity.getCifNumber());
        domain.setCustomerType(parseEnum(CustomerType.class, entity.getCustomerType()));
        domain.setNationalId(entity.getNationalId());
        domain.setNationalIdType(entity.getNationalIdType());
        domain.setTitle(entity.getTitle());
        domain.setFirstName(entity.getFirstName());
        domain.setMiddleName(entity.getMiddleName());
        domain.setLastName(entity.getLastName());
        domain.setFirstNameAr(entity.getFirstNameAr());
        domain.setLastNameAr(entity.getLastNameAr());
        domain.setFullName(entity.getFullName());
        domain.setDateOfBirth(entity.getDateOfBirth());
        domain.setGender(parseEnum(Gender.class, entity.getGender()));
        domain.setNationality(entity.getNationality());
        domain.setResidencyType(parseEnum(ResidencyType.class, entity.getResidencyType()));
        domain.setMobileNumber(entity.getMobileNumber());
        domain.setEmail(entity.getEmail());
        domain.setAddressLine1(entity.getAddressLine1());
        domain.setAddressLine2(entity.getAddressLine2());
        domain.setCity(entity.getCity());
        domain.setRegion(entity.getRegion());
        domain.setPostalCode(entity.getPostalCode());
        domain.setCountry(entity.getCountry());
        domain.setKycStatus(parseEnum(KycStatus.class, entity.getKycStatus()));
        domain.setKycVerifiedAt(toInstant(entity.getKycVerifiedAt()));
        domain.setKycExpiryDate(entity.getKycExpiryDate());
        domain.setNafathVerified(entity.isNafathVerified());
        domain.setNafathTransactionId(entity.getNafathTransactionId());
        domain.setRiskGrade(parseEnum(RiskGrade.class, entity.getRiskGrade()));
        domain.setRiskGradeUpdatedAt(toInstant(entity.getRiskGradeUpdatedAt()));
        domain.setPepFlag(entity.isPepFlag());
        domain.setSanctionsFlag(entity.isSanctionsFlag());
        domain.setPepStatus(parseEnum(com.ksa.financing.customer.domain.model.PepStatus.class, entity.getPepStatus()));
        domain.setKeycloakUserId(entity.getKeycloakUserId());
        domain.setGlobalUid(entity.getGlobalUid());
        domain.setLifecycleStage(parseEnum(LifecycleStage.class, entity.getLifecycleStage()));
        domain.setLifecycleStageChangedAt(toInstant(entity.getLifecycleStageChangedAt()));
        domain.setCustomerSegment(entity.getCustomerSegment());
        domain.setOnboardingFlow(entity.getOnboardingFlow());
        domain.setAcquisitionChannel(entity.getAcquisitionChannel());
        domain.setAcquisitionPartnerId(entity.getAcquisitionPartnerId());
        domain.setActive(entity.isActive());
        domain.setBlockedAt(toInstant(entity.getBlockedAt()));
        domain.setBlockedReason(entity.getBlockedReason());
        domain.setProfilePicture(entity.getProfilePicture());
        domain.setIdempotencyKey(entity.getIdempotencyKey());
        domain.setCreatedAt(toInstant(entity.getCreatedAt()));
        domain.setUpdatedAt(toInstant(entity.getUpdatedAt()));
        domain.setVersion(entity.getVersion());
        return domain;
    }

    // ---- BankAccount mapping ----

    public static BankAccountJpaEntity toEntity(BankAccount domain) {
        if (domain == null) return null;

        BankAccountJpaEntity entity = new BankAccountJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setCustomerId(domain.getCustomerId());
        entity.setBankName(domain.getBankName());
        entity.setBankCode(domain.getBankCode());
        entity.setIban(domain.getIban());
        entity.setAccountHolderName(domain.getAccountHolderName());
        entity.setAccountType(domain.getAccountType());
        entity.setPrimary(domain.isPrimary());
        entity.setSalaryAccount(domain.isSalaryAccount());
        entity.setStatus(domain.getStatus() != null ? domain.getStatus().name() : null);
        entity.setVerifiedAt(toOffsetDateTime(domain.getVerifiedAt()));
        entity.setVerificationMethod(domain.getVerificationMethod());
        entity.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
        entity.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));
        entity.setVersion(domain.getVersion());
        return entity;
    }

    public static BankAccount toDomain(BankAccountJpaEntity entity) {
        if (entity == null) return null;

        BankAccount domain = new BankAccount();
        domain.setId(entity.getId());
        domain.setTenantId(entity.getTenantId());
        domain.setCustomerId(entity.getCustomerId());
        domain.setBankName(entity.getBankName());
        domain.setBankCode(entity.getBankCode());
        domain.setIban(entity.getIban());
        domain.setAccountHolderName(entity.getAccountHolderName());
        domain.setAccountType(entity.getAccountType());
        domain.setPrimary(entity.isPrimary());
        domain.setSalaryAccount(entity.isSalaryAccount());
        domain.setStatus(parseEnum(BankAccountStatus.class, entity.getStatus()));
        domain.setVerifiedAt(toInstant(entity.getVerifiedAt()));
        domain.setVerificationMethod(entity.getVerificationMethod());
        domain.setCreatedAt(toInstant(entity.getCreatedAt()));
        domain.setUpdatedAt(toInstant(entity.getUpdatedAt()));
        domain.setVersion(entity.getVersion());
        return domain;
    }

    // ---- EmploymentInfo mapping ----

    public static EmploymentInfoJpaEntity toEntity(EmploymentInfo domain) {
        if (domain == null) return null;

        EmploymentInfoJpaEntity entity = new EmploymentInfoJpaEntity();
        entity.setId(domain.getId());
        entity.setTenantId(domain.getTenantId());
        entity.setCustomerId(domain.getCustomerId());
        entity.setEmployerName(domain.getEmployerName());
        entity.setEmployerCrNumber(domain.getEmployerCrNumber());
        entity.setEmployerSector(domain.getEmployerSector());
        entity.setEmploymentType(domain.getEmploymentType() != null ? domain.getEmploymentType().name() : null);
        entity.setJobTitle(domain.getJobTitle());
        entity.setStartDate(domain.getStartDate());
        entity.setBasicSalary(domain.getBasicSalary());
        entity.setHousingAllowance(domain.getHousingAllowance());
        entity.setOtherAllowances(domain.getOtherAllowances());
        entity.setGrossSalary(domain.getGrossSalary());
        entity.setDeductions(domain.getDeductions());
        entity.setNetSalary(domain.getNetSalary());
        entity.setCurrency(domain.getCurrency());
        entity.setVerified(domain.isVerified());
        entity.setVerifiedVia(domain.getVerifiedVia());
        entity.setVerifiedAt(toOffsetDateTime(domain.getVerifiedAt()));
        entity.setSalaryBankName(domain.getSalaryBankName());
        entity.setSalaryIban(domain.getSalaryIban());
        entity.setCurrent(domain.isCurrent());
        entity.setEndDate(domain.getEndDate());
        entity.setCreatedAt(toOffsetDateTime(domain.getCreatedAt()));
        entity.setUpdatedAt(toOffsetDateTime(domain.getUpdatedAt()));
        entity.setVersion(domain.getVersion());
        return entity;
    }

    public static EmploymentInfo toDomain(EmploymentInfoJpaEntity entity) {
        if (entity == null) return null;

        EmploymentInfo domain = new EmploymentInfo();
        domain.setId(entity.getId());
        domain.setTenantId(entity.getTenantId());
        domain.setCustomerId(entity.getCustomerId());
        domain.setEmployerName(entity.getEmployerName());
        domain.setEmployerCrNumber(entity.getEmployerCrNumber());
        domain.setEmployerSector(entity.getEmployerSector());
        domain.setEmploymentType(parseEnum(EmploymentType.class, entity.getEmploymentType()));
        domain.setJobTitle(entity.getJobTitle());
        domain.setStartDate(entity.getStartDate());
        domain.setBasicSalary(entity.getBasicSalary());
        domain.setHousingAllowance(entity.getHousingAllowance());
        domain.setOtherAllowances(entity.getOtherAllowances());
        domain.setGrossSalary(entity.getGrossSalary());
        domain.setDeductions(entity.getDeductions());
        domain.setNetSalary(entity.getNetSalary());
        domain.setCurrency(entity.getCurrency());
        domain.setVerified(entity.isVerified());
        domain.setVerifiedVia(entity.getVerifiedVia());
        domain.setVerifiedAt(toInstant(entity.getVerifiedAt()));
        domain.setSalaryBankName(entity.getSalaryBankName());
        domain.setSalaryIban(entity.getSalaryIban());
        domain.setCurrent(entity.isCurrent());
        domain.setEndDate(entity.getEndDate());
        domain.setCreatedAt(toInstant(entity.getCreatedAt()));
        domain.setUpdatedAt(toInstant(entity.getUpdatedAt()));
        domain.setVersion(entity.getVersion());
        return domain;
    }

    // ---- Utility methods ----

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant != null ? instant.atOffset(ZoneOffset.UTC) : null;
    }

    private static Instant toInstant(OffsetDateTime offsetDateTime) {
        return offsetDateTime != null ? offsetDateTime.toInstant() : null;
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
