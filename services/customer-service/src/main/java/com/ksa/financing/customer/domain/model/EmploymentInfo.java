package com.ksa.financing.customer.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class EmploymentInfo {
    private UUID id;
    private UUID tenantId;
    private UUID customerId;
    private String employerName;
    private String employerCrNumber;
    private String employerSector;
    private EmploymentType employmentType;
    private String jobTitle;
    private LocalDate startDate;
    private BigDecimal basicSalary;
    private BigDecimal housingAllowance;
    private BigDecimal otherAllowances;
    private BigDecimal grossSalary;
    private BigDecimal deductions;
    private BigDecimal netSalary;
    private String currency;
    private boolean verified;
    private String verifiedVia;
    private Instant verifiedAt;
    private String salaryBankName;
    private String salaryIban;
    private boolean current;
    private LocalDate endDate;
    private Instant createdAt;
    private Instant updatedAt;
    private int version;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public String getEmployerName() { return employerName; }
    public void setEmployerName(String employerName) { this.employerName = employerName; }
    public String getEmployerCrNumber() { return employerCrNumber; }
    public void setEmployerCrNumber(String employerCrNumber) { this.employerCrNumber = employerCrNumber; }
    public String getEmployerSector() { return employerSector; }
    public void setEmployerSector(String employerSector) { this.employerSector = employerSector; }
    public EmploymentType getEmploymentType() { return employmentType; }
    public void setEmploymentType(EmploymentType employmentType) { this.employmentType = employmentType; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public BigDecimal getBasicSalary() { return basicSalary; }
    public void setBasicSalary(BigDecimal basicSalary) { this.basicSalary = basicSalary; }
    public BigDecimal getHousingAllowance() { return housingAllowance; }
    public void setHousingAllowance(BigDecimal housingAllowance) { this.housingAllowance = housingAllowance; }
    public BigDecimal getOtherAllowances() { return otherAllowances; }
    public void setOtherAllowances(BigDecimal otherAllowances) { this.otherAllowances = otherAllowances; }
    public BigDecimal getGrossSalary() { return grossSalary; }
    public void setGrossSalary(BigDecimal grossSalary) { this.grossSalary = grossSalary; }
    public BigDecimal getDeductions() { return deductions; }
    public void setDeductions(BigDecimal deductions) { this.deductions = deductions; }
    public BigDecimal getNetSalary() { return netSalary; }
    public void setNetSalary(BigDecimal netSalary) { this.netSalary = netSalary; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
    public String getVerifiedVia() { return verifiedVia; }
    public void setVerifiedVia(String verifiedVia) { this.verifiedVia = verifiedVia; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(Instant verifiedAt) { this.verifiedAt = verifiedAt; }
    public String getSalaryBankName() { return salaryBankName; }
    public void setSalaryBankName(String salaryBankName) { this.salaryBankName = salaryBankName; }
    public String getSalaryIban() { return salaryIban; }
    public void setSalaryIban(String salaryIban) { this.salaryIban = salaryIban; }
    public boolean isCurrent() { return current; }
    public void setCurrent(boolean current) { this.current = current; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}
