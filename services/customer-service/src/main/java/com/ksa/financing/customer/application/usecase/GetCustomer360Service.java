package com.ksa.financing.customer.application.usecase;

import com.ksa.financing.customer.application.dto.Customer360Response;
import com.ksa.financing.customer.application.dto.Customer360Response.*;
import com.ksa.financing.customer.application.dto.CustomerResponse;
import com.ksa.financing.customer.domain.model.BankAccount;
import com.ksa.financing.customer.domain.model.Customer;
import com.ksa.financing.customer.domain.model.EmploymentInfo;
import com.ksa.financing.customer.domain.port.in.GetCustomerUseCase;
import com.ksa.financing.customer.domain.port.in.ManageBankAccountsUseCase;
import com.ksa.financing.customer.domain.port.out.*;
import com.ksa.financing.customer.infrastructure.http.HttpKycServiceAdapter;
import com.ksa.financing.customer.infrastructure.http.HttpRiskServiceAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Aggregation service that builds the Customer 360 view by fetching data from
 * multiple microservices: PII Vault, KYC Adapter, Risk Service, Lending Service,
 * Onboarding Workflow, and Wallet Service.
 *
 * All external calls are fault-tolerant — failures return empty/default data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GetCustomer360Service {

    private final GetCustomerUseCase getCustomerUseCase;
    private final ManageBankAccountsUseCase manageBankAccountsUseCase;
    private final EmploymentInfoRepository employmentInfoRepository;
    private final PiiVaultPort piiVaultPort;
    private final WalletPort walletPort;
    private final RiskServicePort riskServicePort;
    private final LendingServicePort lendingServicePort;
    private final KycServicePort kycServicePort;
    private final OnboardingServicePort onboardingServicePort;

    public Customer360Response get360View(UUID customerId, UUID tenantId, boolean superAdmin, String accessToken) {
        log.info("Building Customer 360 view for customerId={}", customerId);

        // 1. Core customer data
        Customer customer = superAdmin
                ? getCustomerUseCase.getById(customerId)
                : getCustomerUseCase.getById(tenantId, customerId);

        String nationalId = customer.getNationalId();
        UUID globalUid = customer.getGlobalUid();
        // Always use customer's own tenantId for downstream calls (superAdmin may pass null)
        UUID effectiveTenantId = customer.getTenantId();

        // 2. PII Vault data
        Map<String, String> piiData = fetchPiiData(globalUid, accessToken);

        // 3. KYC/Yakeen data (names, address, DOB in AR/EN)
        String dob = customer.getDateOfBirth() != null ? customer.getDateOfBirth().toString() : null;
        Map<String, Object> yakeenData = fetchYakeenData(nationalId, dob, accessToken);

        // 4. Risk assessments
        List<Map<String, Object>> riskAssessments = fetchRiskAssessments(nationalId, accessToken);
        Map<String, Object> entityStatus = fetchEntityStatus(nationalId, accessToken);

        // 5. Onboarding status & steps
        Map<String, Object> onboardingStatus = fetchOnboardingStatus(nationalId, accessToken);

        // 6. Loan applications
        List<Map<String, Object>> loanApps = fetchLoanApplications(customerId, accessToken);

        // 7. Bank accounts
        List<BankAccount> bankAccounts = fetchBankAccounts(effectiveTenantId, customerId);

        // 8. Employment info
        List<EmploymentInfo> employments = fetchEmployments(customerId);

        // 9. Wallet IBAN
        String walletIban = fetchWalletIban(effectiveTenantId, customerId);

        // Resolve authoritative risk level from latest assessment (overrides stale customer DB)
        String latestRiskLevel = null;
        Integer latestRiskScore = null;
        boolean latestPepFlag = customer.isPepFlag();
        if (!riskAssessments.isEmpty()) {
            Map<String, Object> latest = riskAssessments.get(0);
            latestRiskLevel = strVal(latest, "riskLevel", strVal(latest, "riskGrade", null));
            latestRiskScore = toInt(latest.get("totalScore") != null ? latest.get("totalScore") : latest.get("overallRiskScore"));
            if (latest.get("pepFlag") != null) {
                latestPepFlag = Boolean.TRUE.equals(latest.get("pepFlag"));
            }
        }

        // Build the response
        return new Customer360Response(
                toCustomerResponse(customer, latestRiskLevel, latestPepFlag),
                buildPersonalInfo(customer, piiData, yakeenData),
                buildAddressInfo(piiData, yakeenData),
                buildKycInfo(customer, latestRiskLevel, latestRiskScore, latestPepFlag, entityStatus),
                buildKycSteps(onboardingStatus),
                buildKycWeightage(riskAssessments, accessToken),
                buildRiskCalculation(riskAssessments, accessToken),
                buildRiskInfo(customer, latestRiskLevel, latestRiskScore, latestPepFlag, entityStatus),
                buildRiskHistory(riskAssessments),
                buildComplianceQuestionHistory(riskAssessments, accessToken),
                buildLoanApplications(loanApps),
                buildBankAccounts(bankAccounts),
                buildEmployments(employments),
                walletIban
        );
    }

    // ==================== Data Fetchers (fault-tolerant) ====================

    private Map<String, String> fetchPiiData(UUID globalUid, String accessToken) {
        if (globalUid == null) return Collections.emptyMap();
        try {
            return piiVaultPort.retrievePii(globalUid, accessToken);
        } catch (Exception e) {
            log.warn("PII vault retrieval failed for globalUid={}: {}", globalUid, e.getMessage());
            return Collections.emptyMap();
        }
    }

    private Map<String, Object> fetchYakeenData(String nationalId, String dateOfBirth, String accessToken) {
        if (nationalId == null) return Collections.emptyMap();
        try {
            if (kycServicePort instanceof HttpKycServiceAdapter httpKyc) {
                return httpKyc.getYakeenData(nationalId, dateOfBirth, accessToken);
            }
            return kycServicePort.getYakeenData(nationalId, accessToken);
        } catch (Exception e) {
            log.warn("Yakeen data fetch failed for NID: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private List<Map<String, Object>> fetchRiskAssessments(String nationalId, String accessToken) {
        if (nationalId == null) return Collections.emptyList();
        try {
            return riskServicePort.getAssessmentsByEntity(nationalId, accessToken);
        } catch (Exception e) {
            log.warn("Risk assessments fetch failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private Map<String, Object> fetchEntityStatus(String nationalId, String accessToken) {
        if (nationalId == null) return Collections.emptyMap();
        try {
            return riskServicePort.getEntityStatus(nationalId, accessToken);
        } catch (Exception e) {
            log.warn("Entity status fetch failed: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private Map<String, Object> fetchOnboardingStatus(String nationalId, String accessToken) {
        if (nationalId == null) return Collections.emptyMap();
        try {
            return onboardingServicePort.getOnboardingStatus(nationalId, accessToken);
        } catch (Exception e) {
            log.warn("Onboarding status fetch failed: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private List<Map<String, Object>> fetchLoanApplications(UUID customerId, String accessToken) {
        try {
            return lendingServicePort.getLoanApplicationsByCustomerId(customerId, accessToken);
        } catch (Exception e) {
            log.warn("Loan applications fetch failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<BankAccount> fetchBankAccounts(UUID tenantId, UUID customerId) {
        try {
            return manageBankAccountsUseCase.getBankAccounts(tenantId, customerId);
        } catch (Exception e) {
            log.warn("Bank accounts fetch failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<EmploymentInfo> fetchEmployments(UUID customerId) {
        try {
            return employmentInfoRepository.findAllByCustomerId(customerId);
        } catch (Exception e) {
            log.warn("Employment fetch failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String fetchWalletIban(UUID tenantId, UUID customerId) {
        try {
            return walletPort.getIbanByCustomerId(tenantId, customerId).orElse(null);
        } catch (Exception e) {
            log.warn("Wallet IBAN fetch failed: {}", e.getMessage());
            return null;
        }
    }

    // ==================== Response Builders ====================

    private PersonalInfo buildPersonalInfo(Customer customer, Map<String, String> pii, Map<String, Object> yakeen) {
        return new PersonalInfo(
                customer.getNationalId(),
                customer.getNationalIdType(),
                strVal(yakeen, "firstName", pii.getOrDefault("firstName", customer.getFirstName())),
                strVal(yakeen, "secondName", pii.getOrDefault("middleName", customer.getMiddleName())),
                strVal(yakeen, "thirdName", null),
                strVal(yakeen, "lastName", pii.getOrDefault("lastName", customer.getLastName())),
                strVal(yakeen, "fullNameEn", pii.getOrDefault("fullName",
                        customer.getFullName() != null ? customer.getFullName() :
                                (customer.getFirstName() + " " + customer.getLastName()))),
                strVal(yakeen, "fullNameAr", pii.getOrDefault("fullNameAr", null)),
                strVal(yakeen, "firstNameAr", customer.getFirstNameAr()),
                strVal(yakeen, "secondNameAr", null),
                strVal(yakeen, "thirdNameAr", null),
                strVal(yakeen, "lastNameAr", customer.getLastNameAr()),
                strVal(yakeen, "dateOfBirthGregorian",
                        customer.getDateOfBirth() != null ? customer.getDateOfBirth().toString() :
                                pii.getOrDefault("dateOfBirth", null)),
                strVal(yakeen, "dateOfBirthHijri", null),
                strVal(yakeen, "gender",
                        customer.getGender() != null ? customer.getGender().name() : pii.getOrDefault("gender", null)),
                strVal(yakeen, "nationality",
                        customer.getNationality() != null ? customer.getNationality() : pii.getOrDefault("nationalityCode", null)),
                strVal(yakeen, "nationalityCode", pii.getOrDefault("nationalityCode", null)),
                strVal(yakeen, "iqamaNumber", null),
                strVal(yakeen, "iqamaIssueDate", null),
                strVal(yakeen, "iqamaExpiryDate", null),
                strVal(yakeen, "iqamaIssuePlace", null),
                customer.getMobileNumber() != null ? customer.getMobileNumber() : pii.getOrDefault("mobile", null),
                customer.getEmail() != null ? customer.getEmail() : pii.getOrDefault("email", null),
                strVal(yakeen, "status", customer.getKycStatus() != null ? customer.getKycStatus().name() : null),
                strVal(yakeen, "verificationDate", customer.getKycVerifiedAt() != null ? customer.getKycVerifiedAt().toString() : null),
                strVal(yakeen, "transactionId", customer.getNafathTransactionId()),
                strVal(yakeen, "requestId", null)
        );
    }

    private AddressInfo buildAddressInfo(Map<String, String> pii, Map<String, Object> yakeen) {
        // Try Yakeen address first, then PII vault
        Map<String, Object> addr = Collections.emptyMap();
        if (yakeen.containsKey("address") && yakeen.get("address") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> a = (Map<String, Object>) yakeen.get("address");
            addr = a;
        }

        return new AddressInfo(
                strVal(addr, "city", pii.getOrDefault("city", null)),
                strVal(addr, "cityId", null),
                strVal(addr, "regionName", pii.getOrDefault("region", null)),
                strVal(addr, "regionId", null),
                strVal(addr, "district", null),
                strVal(addr, "streetName", null),
                strVal(addr, "buildingNumber", pii.getOrDefault("buildingNumber", null)),
                strVal(addr, "additionalNumber", null),
                strVal(addr, "postCode", pii.getOrDefault("postalCode", null)),
                strVal(addr, "shortAddress", null),
                strVal(addr, "locationCoordinates", null),
                true
        );
    }

    private KycInfo buildKycInfo(Customer customer, String riskLevel, Integer riskScore, boolean pepFlag, Map<String, Object> entityStatus) {
        return new KycInfo(
                strVal(entityStatus, "kycId", null),
                customer.getKycStatus() != null ? customer.getKycStatus().name() : "PENDING",
                riskLevel,
                riskScore,
                pepFlag,
                strVal(entityStatus, "complianceStatus", null)
        );
    }

    @SuppressWarnings("unchecked")
    private List<KycStep> buildKycSteps(Map<String, Object> onboardingStatus) {
        if (onboardingStatus.isEmpty() || !onboardingStatus.containsKey("steps")) {
            return Collections.emptyList();
        }

        Object stepsObj = onboardingStatus.get("steps");
        if (!(stepsObj instanceof List)) return Collections.emptyList();

        List<Map<String, Object>> steps = (List<Map<String, Object>>) stepsObj;
        return steps.stream()
                .map(step -> new KycStep(
                        strVal(step, "name", ""),
                        strVal(step, "label", ""),
                        strVal(step, "status", "pending")
                ))
                .toList();
    }

    private List<Map<String, Object>> fetchBreakdownForLatestSession(List<Map<String, Object>> riskAssessments, String accessToken) {
        if (riskAssessments.isEmpty()) return Collections.emptyList();
        Map<String, Object> latest = riskAssessments.get(0);
        String sessionId = strVal(latest, "sessionId", strVal(latest, "id", null));
        if (sessionId == null) return Collections.emptyList();
        try {
            if (riskServicePort instanceof HttpRiskServiceAdapter httpRisk) {
                return httpRisk.getScoreBreakdownList(sessionId, accessToken);
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("Score breakdown fetch failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<KycWeightage> buildKycWeightage(List<Map<String, Object>> riskAssessments, String accessToken) {
        List<Map<String, Object>> breakdown = fetchBreakdownForLatestSession(riskAssessments, accessToken);
        if (breakdown.isEmpty()) return Collections.emptyList();

        return breakdown.stream()
                .map(item -> new KycWeightage(
                        strVal(item, "category", "Kyc"),
                        strVal(item, "factorCode", strVal(item, "answerValue", "")),
                        strVal(item, "parameterId", ""),
                        strVal(item, "factorWeight", "0"),
                        strVal(item, "categoryWeight", "0"),
                        strVal(item, "scoreContribution", "0"),
                        strVal(item, "calculatedAt", "")
                ))
                .toList();
    }

    private RiskCalculation buildRiskCalculation(List<Map<String, Object>> riskAssessments, String accessToken) {
        if (riskAssessments.isEmpty()) return null;

        Map<String, Object> session = riskAssessments.get(0);
        String sessionId = strVal(session, "sessionId", strVal(session, "id", null));
        BigDecimal totalScore = toBigDecimal(session.get("totalScore") != null ? session.get("totalScore") : session.get("overallRiskScore"));
        String riskLevel = strVal(session, "riskLevel", strVal(session, "riskGrade", ""));
        boolean pepFlag = Boolean.TRUE.equals(session.get("pepFlag"));
        boolean eddFlag = Boolean.TRUE.equals(session.get("eddFlag"));
        boolean dominantOverride = Boolean.TRUE.equals(session.get("dominantOverride"));

        // Fetch score breakdown components
        List<Map<String, Object>> breakdown = fetchBreakdownForLatestSession(riskAssessments, accessToken);

        List<RiskCalculation.ScoreComponent> components = breakdown.stream()
                .map(item -> {
                    BigDecimal factorWt = toBigDecimal(item.get("factorWeight"));
                    BigDecimal categoryWt = toBigDecimal(item.get("categoryWeight"));
                    BigDecimal scoreCont = toBigDecimal(item.get("scoreContribution"));

                    // Build human-readable calculation detail
                    String detail = String.format("factorWeight(%s) × categoryWeight(%s) / 100 = %s",
                            factorWt != null ? factorWt : "0",
                            categoryWt != null ? categoryWt : "0",
                            scoreCont != null ? scoreCont : "0");

                    return new RiskCalculation.ScoreComponent(
                            strVal(item, "parameterId", ""),
                            strVal(item, "parameterQuestion", ""),
                            "",
                            strVal(item, "category", ""),
                            strVal(item, "answerValue", ""),
                            strVal(item, "factorCode", ""),
                            factorWt != null ? factorWt : BigDecimal.ZERO,
                            categoryWt != null ? categoryWt : BigDecimal.ZERO,
                            scoreCont != null ? scoreCont : BigDecimal.ZERO,
                            detail,
                            Boolean.TRUE.equals(item.get("eddExcluded"))
                    );
                })
                .toList();

        // Build summary formula
        String componentSum = components.stream()
                .map(c -> c.scoreContribution().toPlainString())
                .reduce((a, b) -> a + " + " + b)
                .orElse("0");
        String formula = String.format(
                "totalScore = SUM(factorWeight × categoryWeight / 100) = %s = %s | riskLevel = %s%s",
                componentSum,
                totalScore != null ? totalScore.toPlainString() : "0",
                riskLevel,
                pepFlag ? " (PEP OVERRIDE)" : dominantOverride ? " (DOMINANT OVERRIDE)" : "");

        return new RiskCalculation(
                sessionId,
                strVal(session, "riskType", ""),
                strVal(session, "status", ""),
                totalScore,
                riskLevel,
                pepFlag,
                eddFlag,
                dominantOverride,
                formula,
                components,
                strVal(session, "updatedAt", strVal(session, "completedAt", ""))
        );
    }

    private RiskInfo buildRiskInfo(Customer customer, String riskLevel, Integer riskScore, boolean pepFlag, Map<String, Object> entityStatus) {
        return new RiskInfo(
                strVal(entityStatus, "kycId", null),
                strVal(entityStatus, "accountStatus", customer.isActive() ? "Active" : "Inactive"),
                riskLevel != null ? riskLevel : (customer.getRiskGrade() != null ? customer.getRiskGrade().name() : null),
                customer.getMobileNumber(),
                riskScore != null ? riskScore.toString() : strVal(entityStatus, "riskScore", null),
                customer.getNationalId(),
                pepFlag,
                customer.getCreatedAt() != null ? customer.getCreatedAt().toString() : null
        );
    }

    private List<RiskHistoryEntry> buildRiskHistory(List<Map<String, Object>> riskAssessments) {
        return riskAssessments.stream()
                .map(a -> new RiskHistoryEntry(
                        strVal(a, "id", strVal(a, "sessionId", "")),
                        strVal(a, "riskType", strVal(a, "assessmentType", "")),
                        toInt(a.get("totalScore") != null ? a.get("totalScore") : a.get("overallRiskScore")),
                        strVal(a, "riskLevel", strVal(a, "riskGrade", "")),
                        strVal(a, "recommendedAction", ""),
                        strVal(a, "status", ""),
                        strVal(a, "completedAt", strVal(a, "scoredAt", "")),
                        null
                ))
                .toList();
    }

    private List<ComplianceQuestionEntry> buildComplianceQuestionHistory(List<Map<String, Object>> riskAssessments, String accessToken) {
        if (riskAssessments.isEmpty()) return Collections.emptyList();

        List<ComplianceQuestionEntry> result = new ArrayList<>();

        for (Map<String, Object> assessment : riskAssessments) {
            String sessionId = strVal(assessment, "sessionId", strVal(assessment, "id", null));
            if (sessionId == null) continue;

            try {
                List<Map<String, Object>> answers = riskServicePort.getAssessmentAnswers(sessionId, accessToken);
                if (answers.isEmpty()) continue;

                String date = strVal(assessment, "completedAt",
                        strVal(assessment, "createdAt", Instant.now().toString()));

                List<ComplianceQuestionEntry.ComplianceAnswer> answerList = answers.stream()
                        .map(a -> new ComplianceQuestionEntry.ComplianceAnswer(
                                strVal(a, "questionTextEn", strVal(a, "questionText", "")),
                                strVal(a, "questionTextAr", ""),
                                strVal(a, "answerValue", strVal(a, "answer", "")),
                                strVal(a, "category", "affordability")
                        ))
                        .toList();

                result.add(new ComplianceQuestionEntry(date, answerList));
            } catch (Exception e) {
                log.warn("Compliance answers fetch failed for session {}: {}", sessionId, e.getMessage());
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private List<LoanApplicationEntry> buildLoanApplications(List<Map<String, Object>> loanApps) {
        return loanApps.stream()
                .map(app -> new LoanApplicationEntry(
                        strVal(app, "applicationNumber", ""),
                        toBigDecimal(app.get("requestedAmount")),
                        strVal(app, "requestedTenureMonths", strVal(app, "tenure", "")) + " months",
                        strVal(app, "applicationType", "Individual"),
                        strVal(app, "status", ""),
                        strVal(app, "productCode", strVal(app, "product", "")),
                        strVal(app, "createdAt", ""),
                        strVal(app, "updatedAt", "")
                ))
                .toList();
    }

    private List<BankAccountEntry> buildBankAccounts(List<BankAccount> accounts) {
        return accounts.stream()
                .map(a -> new BankAccountEntry(
                        a.getId(),
                        a.getBankName(),
                        a.getBankCode(),
                        a.getIban(),
                        a.getAccountHolderName(),
                        a.getAccountType(),
                        a.isPrimary(),
                        a.isSalaryAccount(),
                        a.getStatus() != null ? a.getStatus().name() : null,
                        a.getVerifiedAt() != null ? a.getVerifiedAt().toString() : null,
                        a.getCreatedAt() != null ? a.getCreatedAt().toString() : null
                ))
                .toList();
    }

    private List<EmploymentEntry> buildEmployments(List<EmploymentInfo> employments) {
        return employments.stream()
                .map(e -> new EmploymentEntry(
                        e.getId(),
                        e.getEmployerName(),
                        e.getEmployerCrNumber(),
                        e.getEmployerSector(),
                        e.getEmploymentType() != null ? e.getEmploymentType().name() : null,
                        e.getJobTitle(),
                        e.getNetSalary(),
                        e.getCurrency(),
                        e.isVerified(),
                        e.isCurrent(),
                        e.getCreatedAt() != null ? e.getCreatedAt().toString() : null
                ))
                .toList();
    }

    private CustomerResponse toCustomerResponse(Customer customer, String latestRiskLevel, boolean latestPepFlag) {
        // Use latest risk assessment values over stale customer DB values
        String riskGrade = latestRiskLevel != null ? latestRiskLevel :
                (customer.getRiskGrade() != null ? customer.getRiskGrade().name() : null);
        return new CustomerResponse(
                customer.getId(),
                customer.getCifNumber(),
                customer.getCustomerType() != null ? customer.getCustomerType().name() : null,
                customer.getNationalId(),
                customer.getNationalIdType(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getFirstNameAr(),
                customer.getLastNameAr(),
                customer.getFullName(),
                customer.getDateOfBirth(),
                customer.getGender() != null ? customer.getGender().name() : null,
                customer.getNationality(),
                customer.getResidencyType() != null ? customer.getResidencyType().name() : null,
                customer.getMobileNumber(),
                customer.getEmail(),
                customer.getKycStatus() != null ? customer.getKycStatus().name() : null,
                customer.getLifecycleStage() != null ? customer.getLifecycleStage().name() : null,
                riskGrade,
                latestPepFlag,
                customer.isSanctionsFlag(),
                customer.getGlobalUid(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }

    // ==================== Utility helpers ====================

    private String strVal(Map<String, ?> map, String key, String defaultValue) {
        if (map == null) return defaultValue;
        Object val = map.get(key);
        return val != null ? val.toString() : defaultValue;
    }

    private Integer toInt(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString()); } catch (NumberFormatException e) { return null; }
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return null;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        if (val instanceof Number) return BigDecimal.valueOf(((Number) val).doubleValue());
        try { return new BigDecimal(val.toString()); } catch (NumberFormatException e) { return null; }
    }
}
