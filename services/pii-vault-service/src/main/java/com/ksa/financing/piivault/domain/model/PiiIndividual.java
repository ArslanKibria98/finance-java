package com.ksa.financing.piivault.domain.model;

import java.time.Instant;
import java.util.UUID;

public class PiiIndividual {
    private UUID piiId;
    private UUID globalUid;
    private VaultRegion vaultRegion;
    private String nationalId;
    private String nationalIdType;
    private String fullName;
    private String firstName;
    private String middleName;
    private String lastName;
    private String fullNameAr;
    private String dateOfBirth;
    private String gender;
    private String nationalityCode;
    private String mobile;
    private String email;
    private String alternateMobile;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String region;
    private String postalCode;
    private String countryCode;
    private String employerName;
    private String employerCr;
    private String monthlySalary;
    private String bankName;
    private String iban;
    private String accountHolderName;
    private int encryptionKeyVersion;
    private Instant createdAt;
    private Instant updatedAt;
    private int version;

    // Getters and setters for all fields
    public UUID getPiiId() { return piiId; }
    public void setPiiId(UUID piiId) { this.piiId = piiId; }
    public UUID getGlobalUid() { return globalUid; }
    public void setGlobalUid(UUID globalUid) { this.globalUid = globalUid; }
    public VaultRegion getVaultRegion() { return vaultRegion; }
    public void setVaultRegion(VaultRegion vaultRegion) { this.vaultRegion = vaultRegion; }
    public String getNationalId() { return nationalId; }
    public void setNationalId(String nationalId) { this.nationalId = nationalId; }
    public String getNationalIdType() { return nationalIdType; }
    public void setNationalIdType(String nationalIdType) { this.nationalIdType = nationalIdType; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getFullNameAr() { return fullNameAr; }
    public void setFullNameAr(String fullNameAr) { this.fullNameAr = fullNameAr; }
    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getNationalityCode() { return nationalityCode; }
    public void setNationalityCode(String nationalityCode) { this.nationalityCode = nationalityCode; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAlternateMobile() { return alternateMobile; }
    public void setAlternateMobile(String alternateMobile) { this.alternateMobile = alternateMobile; }
    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public String getEmployerName() { return employerName; }
    public void setEmployerName(String employerName) { this.employerName = employerName; }
    public String getEmployerCr() { return employerCr; }
    public void setEmployerCr(String employerCr) { this.employerCr = employerCr; }
    public String getMonthlySalary() { return monthlySalary; }
    public void setMonthlySalary(String monthlySalary) { this.monthlySalary = monthlySalary; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }
    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }
    public int getEncryptionKeyVersion() { return encryptionKeyVersion; }
    public void setEncryptionKeyVersion(int encryptionKeyVersion) { this.encryptionKeyVersion = encryptionKeyVersion; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
