package com.ksa.financing.customer.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Comprehensive Customer 360 View response aggregating data from
 * Customer Service, PII Vault, KYC Adapter, Risk Service, Lending Service,
 * Onboarding Workflow, and Wallet Service.
 */
public record Customer360Response(
        CustomerResponse customer,
        CountryConfig countryConfig,
        PersonalInfo personalInfo,
        AddressInfo addressInfo,
        KycInfo kycInfo,
        List<KycStep> kycSteps,
        List<KycWeightage> kycWeightageData,
        RiskCalculation riskCalculation,
        RiskInfo riskInfo,
        List<RiskHistoryEntry> riskHistory,
        List<ComplianceQuestionEntry> complianceQuestionHistory,
        List<LoanApplicationEntry> loanApplications,
        List<BankAccountEntry> bankAccounts,
        List<EmploymentEntry> employments,
        String walletIban,
        String walletAccountNumber
) {

    // ==================== Country Configuration ====================
    public record CountryConfig(
            String countryCode,
            String countryName,
            String countryNameAr,
            String currencyCode,
            String flagEmoji,
            String dialCode,
            String nationalityEn,
            String nationalityAr,
            List<String> supportedIdTypes,
            String defaultIdType,
            List<String> kycProviders,
            int totalOnboardingSteps
    ) {}


    // ==================== Personal Information (PII Vault + Yakeen) ====================
    public record PersonalInfo(
            String nationalId,
            String nationalIdType,
            String firstName,
            String secondName,
            String thirdName,
            String lastName,
            String fullNameEn,
            String fullNameAr,
            String firstNameAr,
            String secondNameAr,
            String thirdNameAr,
            String lastNameAr,
            String dateOfBirthGregorian,
            String dateOfBirthHijri,
            String gender,
            String nationality,
            String nationalityCode,
            String iqamaNumber,
            String iqamaIssueDate,
            String iqamaExpiryDate,
            String iqamaIssuePlace,
            String mobile,
            String email,
            String status,
            String verificationDate,
            String transactionId,
            String requestId
    ) {}

    // ==================== Address Information (Yakeen / PII Vault) ====================
    public record AddressInfo(
            String city,
            String cityId,
            String regionName,
            String regionId,
            String district,
            String streetName,
            String buildingNumber,
            String additionalNumber,
            String postCode,
            String shortAddress,
            String locationCoordinates,
            boolean isPrimaryAddress
    ) {}

    // ==================== KYC Information ====================
    public record KycInfo(
            String kycId,
            String kycStatus,
            String riskLevel,
            Integer riskScore,
            boolean isPep,
            String complianceStatus
    ) {}

    // ==================== KYC Steps ====================
    public record KycStep(
            String name,
            String label,
            String status
    ) {}

    // ==================== KYC Data with Weightage ====================
    public record KycWeightage(
            String category,
            String lovType,
            String questionId,
            String factorWeight,
            String categoryWeight,
            String calculatedScore,
            String calculatedAt
    ) {}

    // ==================== Risk Calculation (Full Formula) ====================
    public record RiskCalculation(
            String sessionId,
            String riskType,
            String status,
            BigDecimal totalScore,
            String riskLevel,
            boolean pepFlag,
            boolean eddFlag,
            boolean dominantOverride,
            String formula,
            List<ScoreComponent> components,
            String scoredAt
    ) {
        public record ScoreComponent(
                String parameterId,
                String question,
                String questionAr,
                String category,
                String answerValue,
                String factorCode,
                BigDecimal factorWeight,
                BigDecimal categoryWeight,
                BigDecimal scoreContribution,
                String calculationDetail,
                boolean eddExcluded
        ) {}
    }

    // ==================== Risk Information ====================
    public record RiskInfo(
            String kycId,
            String status,
            String riskLevel,
            String phone,
            String riskScore,
            String nationalId,
            boolean isPep,
            String createdAt
    ) {}

    // ==================== Risk History ====================
    public record RiskHistoryEntry(
            String assessmentId,
            String assessmentType,
            Integer overallRiskScore,
            String riskGrade,
            String recommendedAction,
            String status,
            String completedAt,
            Map<String, Object> assessmentData
    ) {}

    // ==================== Compliance Question History ====================
    public record ComplianceQuestionEntry(
            String date,
            List<ComplianceAnswer> answers
    ) {
        public record ComplianceAnswer(
                String questionEn,
                String questionAr,
                String answer,
                String category,
                String factorWeightPct,   // % of category this factor represents
                String categoryWeight,    // max points this category contributes
                String scoreContribution, // actual score earned = categoryWeight × factorWeightPct / 100
                String calculationDetail  // human-readable formula e.g. "16 × 25% / 100 = 4.00 pts"
        ) {}
    }

    // ==================== Loan Applications ====================
    public record LoanApplicationEntry(
            String applicationNumber,
            BigDecimal amount,
            String duration,
            String type,
            String status,
            String product,
            String createdAt,
            String updatedAt
    ) {}

    // ==================== Bank Accounts ====================
    public record BankAccountEntry(
            UUID id,
            String bankName,
            String bankCode,
            String iban,
            String accountHolderName,
            String accountType,
            boolean isPrimary,
            boolean isSalaryAccount,
            String status,
            String verifiedAt,
            String createdAt
    ) {}

    // ==================== Employment ====================
    public record EmploymentEntry(
            UUID id,
            String employerName,
            String employerCrNumber,
            String employerSector,
            String employmentType,
            String jobTitle,
            BigDecimal netSalary,
            String currency,
            boolean verified,
            boolean isCurrent,
            String createdAt
    ) {}
}
