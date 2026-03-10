package com.ksa.financing.customer.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "employment_info")
@Getter
@Setter
// NOTE: Filename should be EmploymentInfoJpaEntity.java - renamed class per naming conventions
public class EmploymentInfoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "employer_name", nullable = false, length = 255)
    private String employerName;

    @Column(name = "employer_cr_number", length = 20)
    private String employerCrNumber;

    @Column(name = "employer_sector", length = 100)
    private String employerSector;

    @Column(name = "employment_type", nullable = false)
    private String employmentType;

    @Column(name = "job_title", length = 100)
    private String jobTitle;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "basic_salary", precision = 19, scale = 4)
    private BigDecimal basicSalary;

    @Column(name = "housing_allowance", precision = 19, scale = 4)
    private BigDecimal housingAllowance;

    @Column(name = "other_allowances", precision = 19, scale = 4)
    private BigDecimal otherAllowances;

    @Column(name = "gross_salary", precision = 19, scale = 4)
    private BigDecimal grossSalary;

    @Column(name = "deductions", precision = 19, scale = 4)
    private BigDecimal deductions;

    @Column(name = "net_salary", nullable = false, precision = 19, scale = 4)
    private BigDecimal netSalary;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "is_verified", nullable = false)
    private boolean verified;

    @Column(name = "verified_via", length = 50)
    private String verifiedVia;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @Column(name = "salary_bank_name", length = 100)
    private String salaryBankName;

    @Column(name = "salary_iban", length = 34)
    private String salaryIban;

    @Column(name = "is_current", nullable = false)
    private boolean current;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private int version;
}
