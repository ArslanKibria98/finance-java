package com.ksa.financing.customer.application.usecase;

import com.ksa.financing.customer.application.dto.Customer360Response;
import com.ksa.financing.customer.application.dto.Customer360Response.*;
import com.ksa.financing.customer.application.dto.CustomerResponse;
import com.ksa.financing.customer.domain.model.BankAccount;
import com.ksa.financing.customer.domain.model.Customer;
import com.ksa.financing.customer.domain.model.EmploymentInfo;
import com.ksa.financing.customer.domain.port.in.GetCustomerUseCase;
import com.ksa.financing.customer.domain.port.in.ManageBankAccountsUseCase;
import com.ksa.financing.customer.domain.model.SupportedCountry;
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
    private final CountryConfigRepository countryConfigRepository;

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

        // 4. Risk assessments — merge session-based + AML-based
        List<Map<String, Object>> riskAssessments = fetchRiskAssessments(nationalId, customerId, accessToken);
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
            // Support both session-based (riskLevel/riskGrade) and AML-based (riskLevel) field names
            latestRiskLevel = strVal(latest, "riskLevel", strVal(latest, "riskGrade", null));
            latestRiskScore = toInt(latest.get("totalScore") != null ? latest.get("totalScore") : latest.get("overallRiskScore"));
            // For AML assessments, sessionId is stored as assessmentId
            if (!latest.containsKey("sessionId") && latest.containsKey("assessmentId")) {
                latest.put("sessionId", latest.get("assessmentId"));
            }
            if (latest.get("pepFlag") != null) {
                latestPepFlag = Boolean.TRUE.equals(latest.get("pepFlag"));
            }
        }

        // 10. Country config
        String countryCode = resolveCountryCode(customer.getCountry());
        Customer360Response.CountryConfig countryConfig = buildCountryConfig(countryCode);

        // Build the response
        return new Customer360Response(
                toCustomerResponse(customer, latestRiskLevel, latestPepFlag),
                countryConfig,
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

    private List<Map<String, Object>> fetchRiskAssessments(String nationalId, UUID customerId, String accessToken) {
        List<Map<String, Object>> sessionBased = Collections.emptyList();
        if (nationalId != null) {
            try {
                sessionBased = riskServicePort.getAssessmentsByEntity(nationalId, accessToken);
            } catch (Exception e) {
                log.warn("Risk session assessments fetch failed: {}", e.getMessage());
            }
        }

        // Also fetch AML assessments (from aml_risk_assessments table) by customer UUID
        List<Map<String, Object>> amlBased = Collections.emptyList();
        if (customerId != null && riskServicePort instanceof HttpRiskServiceAdapter httpRisk) {
            amlBased = httpRisk.getAmlAssessmentsByCustomerId(customerId.toString());
        }

        log.debug("fetchRiskAssessments: sessionBased.size={}, amlBased.size={}", sessionBased.size(), amlBased.size());
        if (!amlBased.isEmpty() && sessionBased.isEmpty()) {
            return amlBased;
        }
        if (!sessionBased.isEmpty()) {
            return sessionBased;
        }
        return Collections.emptyList();
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

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchBreakdownForLatestSession(List<Map<String, Object>> riskAssessments, String accessToken) {
        if (riskAssessments.isEmpty()) return Collections.emptyList();
        Map<String, Object> latest = riskAssessments.get(0);

        // AML assessments embed breakdown directly — no need for separate API call
        if (latest.containsKey("breakdown") && latest.get("breakdown") instanceof List) {
            return (List<Map<String, Object>>) latest.get("breakdown");
        }

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

        String assessedAt = riskAssessments.isEmpty() ? "" :
                strVal(riskAssessments.get(0), "assessedAt",
                strVal(riskAssessments.get(0), "completedAt", ""));

        return breakdown.stream()
                .map(item -> {
                    // Support both AML field names (categoryCode/factorWeightPct/rating)
                    // and session field names (category/factorWeight/scoreContribution)
                    String category     = strVal(item, "category", strVal(item, "categoryName", strVal(item, "categoryCode", "")));
                    String lovType      = strVal(item, "factorCode", strVal(item, "matchedFactor", strVal(item, "answerValue", "")));
                    String questionId   = strVal(item, "parameterId", strVal(item, "categoryCode", ""));
                    String factorWtPct  = strVal(item, "factorWeight", strVal(item, "factorWeightPct", "0"));
                    String categoryWt   = strVal(item, "categoryWeight", "0");
                    // calculatedScore = categoryWeight × factorWeightPct / 100
                    Object ratingRaw    = item.get("scoreContribution") != null ? item.get("scoreContribution") : item.get("rating");
                    String calcScore;
                    if (ratingRaw != null) {
                        calcScore = ratingRaw.toString();
                    } else {
                        // Derive: categoryWeight × factorWeightPct / 100
                        try {
                            BigDecimal cw = new BigDecimal(categoryWt);
                            BigDecimal fw = new BigDecimal(factorWtPct);
                            calcScore = cw.multiply(fw).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP).toPlainString();
                        } catch (Exception e) {
                            calcScore = "0";
                        }
                    }
                    return new KycWeightage(category, lovType, questionId, factorWtPct, categoryWt, calcScore, assessedAt);
                })
                .toList();
    }

    private RiskCalculation buildRiskCalculation(List<Map<String, Object>> riskAssessments, String accessToken) {
        if (riskAssessments.isEmpty()) return null;

        Map<String, Object> session = riskAssessments.get(0);
        String sessionId = strVal(session, "sessionId", strVal(session, "assessmentId", strVal(session, "id", null)));
        BigDecimal totalScore = toBigDecimal(session.get("totalScore") != null ? session.get("totalScore") : session.get("overallRiskScore"));
        String riskLevel = strVal(session, "riskLevel", strVal(session, "riskGrade", ""));
        boolean pepFlag = Boolean.TRUE.equals(session.get("pepFlag"));
        boolean eddFlag = Boolean.TRUE.equals(session.get("eddFlag"));
        boolean dominantOverride = Boolean.TRUE.equals(session.get("dominantOverride"));
        String dominantCategory = strVal(session, "dominantCategory", null);

        // Fetch score breakdown components (embedded in AML or via separate API for sessions)
        List<Map<String, Object>> breakdown = fetchBreakdownForLatestSession(riskAssessments, accessToken);

        List<RiskCalculation.ScoreComponent> components = breakdown.stream()
                .map(item -> {
                    // Support both AML field names and session-based field names
                    String category    = strVal(item, "category", strVal(item, "categoryCode", ""));
                    String question    = strVal(item, "parameterQuestion", strVal(item, "categoryName", category));
                    String answerValue = strVal(item, "answerValue", strVal(item, "matchedFactor", ""));
                    String factorCode  = strVal(item, "factorCode", strVal(item, "matchedFactor", strVal(item, "categoryCode", "")));
                    String paramId     = strVal(item, "parameterId", strVal(item, "categoryCode", ""));

                    // factorWeightPct = what % of the category this factor represents
                    BigDecimal factorWtPct = toBigDecimal(item.get("factorWeight") != null ? item.get("factorWeight") : item.get("factorWeightPct"));
                    // categoryWeight = max points this category contributes
                    BigDecimal categoryWt  = toBigDecimal(item.get("categoryWeight"));
                    // rating = actual score earned = categoryWeight × factorWeightPct / 100
                    BigDecimal scoreCont   = toBigDecimal(item.get("scoreContribution") != null ? item.get("scoreContribution") : item.get("rating"));

                    // Derive missing values
                    if (scoreCont == null && factorWtPct != null && categoryWt != null) {
                        scoreCont = categoryWt.multiply(factorWtPct).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
                    }

                    String detail = String.format("%s × %s%% / 100 = %s pts  [category=%s, factor=%s]",
                            categoryWt != null ? categoryWt.toPlainString() : "?",
                            factorWtPct != null ? factorWtPct.toPlainString() : "?",
                            scoreCont != null ? scoreCont.toPlainString() : "?",
                            category, answerValue);

                    return new RiskCalculation.ScoreComponent(
                            paramId, question, "", category, answerValue, factorCode,
                            factorWtPct != null ? factorWtPct : BigDecimal.ZERO,
                            categoryWt != null ? categoryWt : BigDecimal.ZERO,
                            scoreCont != null ? scoreCont : BigDecimal.ZERO,
                            detail,
                            Boolean.TRUE.equals(item.get("eddExcluded"))
                    );
                })
                .toList();

        // Build EastNets formula string
        String componentTerms = components.stream()
                .map(c -> String.format("(%s×%s%%/100=%s[%s])",
                        c.categoryWeight().toPlainString(),
                        c.factorWeight().toPlainString(),
                        c.scoreContribution().toPlainString(),
                        c.category()))
                .reduce((a, b) -> a + " + " + b)
                .orElse("N/A");

        String formula;
        if (dominantOverride && dominantCategory != null) {
            formula = String.format(
                    "EastNets AML Score = DOMINANT_OVERRIDE triggered by [%s] → totalScore forced to %s (riskLevel=%s). " +
                    "Base formula: Σ(categoryWeight × factorWeightPct / 100). Components: %s",
                    dominantCategory,
                    totalScore != null ? totalScore.toPlainString() : "999",
                    riskLevel,
                    componentTerms);
        } else {
            BigDecimal sum = components.stream()
                    .map(RiskCalculation.ScoreComponent::scoreContribution)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            formula = String.format(
                    "EastNets AML Score = Σ(categoryWeight × factorWeightPct / 100) = %s = %s | riskLevel=%s",
                    componentTerms, sum.toPlainString(), riskLevel);
        }

        String scoredAt = strVal(session, "assessedAt", strVal(session, "updatedAt", strVal(session, "completedAt", "")));
        String riskType = strVal(session, "riskType", "AML_KYC");
        String status   = strVal(session, "status", dominantOverride ? "DOMINANT_OVERRIDE" : "COMPLETED");

        return new RiskCalculation(sessionId, riskType, status, totalScore, riskLevel,
                pepFlag, eddFlag, dominantOverride, formula, components, scoredAt);
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

    @SuppressWarnings("unchecked")
    private List<ComplianceQuestionEntry> buildComplianceQuestionHistory(List<Map<String, Object>> riskAssessments, String accessToken) {
        if (riskAssessments.isEmpty()) return Collections.emptyList();

        List<ComplianceQuestionEntry> result = new ArrayList<>();

        for (Map<String, Object> assessment : riskAssessments) {
            String date = strVal(assessment, "completedAt",
                    strVal(assessment, "assessedAt", strVal(assessment, "createdAt", Instant.now().toString())));

            // AML assessments embed inputData — build compliance Q&A + weightage from it
            if (assessment.containsKey("inputData") && assessment.get("inputData") instanceof Map) {
                Map<String, Object> inputData = (Map<String, Object>) assessment.get("inputData");

                // Extract breakdown for weightage lookup
                List<Map<String, Object>> breakdown = Collections.emptyList();
                if (assessment.containsKey("breakdown") && assessment.get("breakdown") instanceof List) {
                    breakdown = (List<Map<String, Object>>) assessment.get("breakdown");
                }

                // Build category → breakdown item index for O(1) lookup
                Map<String, Map<String, Object>> breakdownByCategory = new HashMap<>();
                for (Map<String, Object> item : breakdown) {
                    String catCode = strVal(item, "categoryCode", strVal(item, "category", ""));
                    if (!catCode.isBlank()) {
                        breakdownByCategory.put(catCode, item);
                    }
                }

                List<ComplianceQuestionEntry.ComplianceAnswer> answers =
                        buildAnswersFromAmlInput(inputData, breakdownByCategory);
                if (!answers.isEmpty()) {
                    result.add(new ComplianceQuestionEntry(date, answers));
                }
                continue;
            }

            // Session-based assessments — fetch answers via API
            String sessionId = strVal(assessment, "sessionId", strVal(assessment, "id", null));
            if (sessionId == null) continue;
            try {
                List<Map<String, Object>> answers = riskServicePort.getAssessmentAnswers(sessionId, accessToken);
                if (answers.isEmpty()) continue;
                List<ComplianceQuestionEntry.ComplianceAnswer> answerList = answers.stream()
                        .map(a -> {
                            String fwPct = strVal(a, "factorWeight", strVal(a, "factorWeightPct", null));
                            String catWt = strVal(a, "categoryWeight", null);
                            String scoreC = strVal(a, "scoreContribution", strVal(a, "rating", null));
                            String calcDetail = buildCalcDetail(fwPct, catWt, scoreC);
                            return new ComplianceQuestionEntry.ComplianceAnswer(
                                    strVal(a, "questionTextEn", strVal(a, "questionText", "")),
                                    strVal(a, "questionTextAr", ""),
                                    strVal(a, "answerValue", strVal(a, "answer", "")),
                                    strVal(a, "category", "affordability"),
                                    fwPct, catWt, scoreC, calcDetail
                            );
                        })
                        .toList();
                result.add(new ComplianceQuestionEntry(date, answerList));
            } catch (Exception e) {
                log.warn("Compliance answers fetch failed for session {}: {}", sessionId, e.getMessage());
            }
        }

        return result;
    }

    // Maps inputData field → [questionEn, questionAr, categoryCode matching AML breakdown]
    private static final Map<String, String[]> AML_FIELD_LABELS = new java.util.LinkedHashMap<>() {{
        put("nationality",      new String[]{"Nationality", "\u0627\u0644\u062c\u0646\u0633\u064a\u0629", "NATIONALITY"});
        put("cityName",         new String[]{"City of Residence", "\u0645\u062f\u064a\u0646\u0629 \u0627\u0644\u0625\u0642\u0627\u0645\u0629", "GEOGRAPHICAL_LOCATION"});
        put("occupationCode",   new String[]{"Occupation / Employment", "\u0627\u0644\u0645\u0647\u0646\u0629 / \u0627\u0644\u0648\u0638\u064a\u0641\u0629", "OCCUPATIONS"});
        put("monthlyIncome",    new String[]{"Monthly Income (SAR)", "\u0627\u0644\u062f\u062e\u0644 \u0627\u0644\u0634\u0647\u0631\u064a (\u0631.\u0633)", "INCOME_RANGE"});
        put("sourceOfIncome",   new String[]{"Source of Income", "\u0645\u0635\u062f\u0631 \u0627\u0644\u062f\u062e\u0644", "SOURCE_OF_INCOME"});
        put("productRiskTier",  new String[]{"Product Risk Tier", "\u0645\u0633\u062a\u0648\u0649 \u0645\u062e\u0627\u0637\u0631\u0629 \u0627\u0644\u0645\u0646\u062a\u062c", "PRODUCT_SERVICES"});
        put("isPep",            new String[]{"Politically Exposed Person (PEP)?", "\u0647\u0644 \u0623\u0646\u062a \u0634\u062e\u0635 \u0628\u0627\u0631\u0632 \u0633\u064a\u0627\u0633\u064a\u0627\u064b\u061f", "PEP"});
        put("isOnInternalList", new String[]{"On Internal Watchlist?", "\u0647\u0644 \u0623\u0646\u062a \u0639\u0644\u0649 \u0642\u0627\u0626\u0645\u0629 \u0627\u0644\u0645\u0631\u0627\u0642\u0628\u0629 \u0627\u0644\u062f\u0627\u062e\u0644\u064a\u0629\u061f", "AML_SCREENING"});
    }};

    private List<ComplianceQuestionEntry.ComplianceAnswer> buildAnswersFromAmlInput(
            Map<String, Object> inputData,
            Map<String, Map<String, Object>> breakdownByCategory) {

        List<ComplianceQuestionEntry.ComplianceAnswer> answers = new ArrayList<>();
        // Use a stable order (insertion order via LinkedHashMap-compatible iteration)
        String[] orderedFields = {"nationality","cityName","occupationCode","monthlyIncome",
                                  "sourceOfIncome","productRiskTier","isPep","isOnInternalList"};
        for (String field : orderedFields) {
            if (!AML_FIELD_LABELS.containsKey(field)) continue;
            Object val = inputData.get(field);
            if (val == null) continue;

            String[] labels = AML_FIELD_LABELS.get(field);
            String category = labels[2];

            // Look up breakdown weightage for this category
            Map<String, Object> bItem = breakdownByCategory.get(category);
            String fwPct   = null;
            String catWt   = null;
            String scoreC  = null;
            String calcDetail = null;

            if (bItem != null) {
                fwPct  = strVal(bItem, "factorWeightPct", strVal(bItem, "factorWeight", null));
                catWt  = strVal(bItem, "categoryWeight", null);
                Object ratingRaw = bItem.get("rating") != null ? bItem.get("rating") : bItem.get("scoreContribution");
                if (ratingRaw != null) {
                    scoreC = ratingRaw.toString();
                } else if (fwPct != null && catWt != null) {
                    // Derive: categoryWeight × factorWeightPct / 100
                    try {
                        BigDecimal cw = new BigDecimal(catWt);
                        BigDecimal fw = new BigDecimal(fwPct);
                        scoreC = cw.multiply(fw)
                                   .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP)
                                   .toPlainString();
                    } catch (Exception ignored) {}
                }
                calcDetail = buildCalcDetail(fwPct, catWt, scoreC);
            }

            answers.add(new ComplianceQuestionEntry.ComplianceAnswer(
                    labels[0], labels[1], val.toString(), category,
                    fwPct, catWt, scoreC, calcDetail
            ));
        }
        return answers;
    }

    /** Builds a human-readable formula string: "catWt × fwPct% / 100 = score pts" */
    private String buildCalcDetail(String fwPct, String catWt, String scoreC) {
        if (fwPct == null && catWt == null) return null;
        return String.format("%s × %s%% / 100 = %s pts",
                catWt  != null ? catWt  : "?",
                fwPct  != null ? fwPct  : "?",
                scoreC != null ? scoreC : "?");
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
        String profilePictureUrl = customer.getProfilePicture() != null
                ? "/api/v1/customers/" + customer.getId() + "/profile-picture"
                : null;
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
                profilePictureUrl,
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }

    private Customer360Response.CountryConfig buildCountryConfig(String countryCode) {
        try {
            var country = countryConfigRepository.findCountryByCode(countryCode).orElse(null);
            int totalSteps = countryConfigRepository.findStepsByCountryCode(countryCode).size();
            if (country != null) {
                return new Customer360Response.CountryConfig(
                        country.countryCode(),
                        country.countryName(),
                        country.countryNameAr(),
                        country.currencyCode(),
                        country.flagEmoji(),
                        country.dialCode(),
                        country.nationalityEn(),
                        country.nationalityAr(),
                        List.of(country.idTypes().split(",")),
                        country.defaultIdType(),
                        List.of(country.kycProviders().split(",")),
                        totalSteps
                );
            }
        } catch (Exception e) {
            log.warn("Country config fetch failed for {}: {}", countryCode, e.getMessage());
        }
        return new Customer360Response.CountryConfig(countryCode, countryCode, null, "SAR", null, null, null, null, List.of(), "", List.of(), 0);
    }

    // ISO alpha-2 to alpha-3 mapping for common countries
    private static final Map<String, String> COUNTRY_CODE_MAP = Map.of(
            "SA", "SAU", "AE", "ARE", "PK", "PAK", "EG", "EGY", "MY", "MYS",
            "BH", "BHR", "KW", "KWT", "OM", "OMN", "QA", "QAT", "JO", "JOR"
    );

    private String resolveCountryCode(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) return "SAU";
        String upper = rawCode.toUpperCase().trim();
        if (upper.length() == 2) {
            return COUNTRY_CODE_MAP.getOrDefault(upper, upper);
        }
        return upper;
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
