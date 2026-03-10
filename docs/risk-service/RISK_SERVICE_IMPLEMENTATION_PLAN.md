# Risk Service — Complete Implementation Plan
# AML Risk Engine + Fraud Monitoring System
> **Version**: 1.0 | **Date**: 2026-03-09
> **Source Documents**: AML Risk Schema (Excel) + Finova Fraud Monitoring HLD v1 (Word)
> **Goal**: Build an outsource-ready, multi-tenant AML + Fraud Monitoring system

---

## TABLE OF CONTENTS

1. [Existing System (KEEP — No Duplication)](#1-existing-system)
2. [Final Package Structure](#2-final-package-structure)
3. [New Domain Models](#3-new-domain-models)
4. [New Database Migrations](#4-new-database-migrations)
5. [New API Endpoints](#5-new-api-endpoints)
6. [New Use Cases & Services](#6-new-use-cases--services)
7. [New Infrastructure Components](#7-new-infrastructure-components)
8. [Rule Engine Architecture](#8-rule-engine-architecture)
9. [Kafka Event Integration](#9-kafka-event-integration)
10. [Sprint Breakdown](#10-sprint-breakdown)

---

## 1. EXISTING SYSTEM (KEEP — No Duplication)

### Existing Controllers (4) — DO NOT TOUCH
| Controller | Base Path | Endpoints | Purpose |
|------------|-----------|-----------|---------|
| `InternalChecksController` | `/api/v1/risk/internal-checks` | POST | 9-step onboarding fraud checks |
| `AmlRiskScoreController` | `/api/v1/risk/aml-score` | POST | AML risk scoring (EastNets model) |
| `BlacklistController` | `/api/v1/risk/blacklist` | 8 endpoints (NID + Mobile CRUD) | Blacklist management |
| `CreditScoringController` | `/api/v1/risk/credit-scoring` | 4 endpoints (field defs + product criteria) | Credit scoring config |

### Existing Domain Models (KEEP)
- `InternalCheckRequest`, `InternalCheckResult`, `CheckStepResult`, `CheckDecision`
- `RiskLevel`, `RiskAssessmentStatus`, `InternalCheckStep`, `CifStatus`, `BlacklistStatus`
- `NidBlacklistEntry`, `MobileBlacklistEntry`, `AuditEventType`
- `aml/*` — `AmlRiskScore`, `AmlScoringInput`, `AmlRiskCategory`, `AmlRiskCategoryFactor`, `AmlRiskThreshold`, `AmlCategoryScoreBreakdown`, `AmlRiskLevel`, `AmlCategoryType`, `FatfCategory`, `OccupationRiskLevel`
- `credit/*` — `CreditScoringFieldDefinition`, `CreditScoringCriteria`, `CreditScoringRule`, `CreditScoringOperator`

### Existing Use Cases (KEEP)
- `RunInternalChecksService` (9-step pipeline)
- `CalculateAmlRiskScoreService` (AML scoring)
- `ManageBlacklistUseCaseImpl` (NID/mobile blacklist)
- `GetCreditScoringFieldsService`, `ManageCreditScoringService` (credit scoring)

### Existing Infrastructure Checks (KEEP)
- `NidFormatValidatorImpl`, `BlacklistWatchlistCheckImpl`, `FraudHistoryCheckImpl`
- `DeviceFingerprintCheckImpl`, `VelocityCheckImpl`, `CifLookupCheckImpl`
- `DuplicateMobileCheckImpl`, `AccountLockCheckImpl`, `InternalSanctionsCheckImpl`
- `RiskScoreCalculatorImpl`

### Existing DB Migrations V1-V7 (KEEP)
- V1: Core schema (risk_assessments, fraud_signal_logs, screening_results, watchlist_entries, velocity_checks, device_registry, account_locks, customers_view, change_log, outbox_events)
- V2: FDW to customer_db
- V3: Blacklist tables (nid_blacklist, mobile_blacklist)
- V4: AML scoring schema (categories, factors, thresholds, FATF, cities, occupations, income, source)
- V5: AML reference data seed
- V6: City risk data seed
- V7: Credit scoring schema

---

## 2. FINAL PACKAGE STRUCTURE

```
services/risk-service/src/main/java/com/ksa/financing/risk/
│
├── domain/
│   ├── model/
│   │   ├── InternalCheckRequest.java              ← EXISTING
│   │   ├── InternalCheckResult.java               ← EXISTING
│   │   ├── InternalCheckStep.java                 ← EXISTING
│   │   ├── CheckStepResult.java                   ← EXISTING
│   │   ├── CheckDecision.java                     ← EXISTING
│   │   ├── RiskLevel.java                         ← EXISTING
│   │   ├── RiskAssessmentStatus.java              ← EXISTING
│   │   ├── AuditEventType.java                    ← EXISTING
│   │   ├── CifStatus.java                         ← EXISTING
│   │   ├── BlacklistStatus.java                   ← EXISTING
│   │   ├── NidBlacklistEntry.java                 ← EXISTING
│   │   ├── MobileBlacklistEntry.java              ← EXISTING
│   │   │
│   │   ├── aml/                                   ← EXISTING (8 files)
│   │   │   ├── AmlRiskScore.java                  ← EXISTING
│   │   │   ├── AmlScoringInput.java               ← EXISTING
│   │   │   ├── AmlRiskCategory.java               ← EXISTING
│   │   │   ├── AmlRiskCategoryFactor.java         ← EXISTING
│   │   │   ├── AmlCategoryScoreBreakdown.java     ← EXISTING
│   │   │   ├── AmlRiskThreshold.java              ← EXISTING
│   │   │   ├── AmlRiskLevel.java                  ← EXISTING
│   │   │   ├── AmlCategoryType.java               ← EXISTING
│   │   │   ├── FatfCategory.java                  ← EXISTING
│   │   │   └── OccupationRiskLevel.java           ← EXISTING
│   │   │
│   │   ├── credit/                                ← EXISTING (4 files)
│   │   │   ├── CreditScoringFieldDefinition.java  ← EXISTING
│   │   │   ├── CreditScoringCriteria.java         ← EXISTING
│   │   │   ├── CreditScoringRule.java             ← EXISTING
│   │   │   └── CreditScoringOperator.java         ← EXISTING
│   │   │
│   │   ├── edd/                                   ← NEW (AML Excel — EDD)
│   │   │   ├── EddRequirement.java                ← NEW record
│   │   │   ├── EddSubmission.java                 ← NEW record
│   │   │   ├── EddQuestion.java                   ← NEW record
│   │   │   ├── EddAnswer.java                     ← NEW record
│   │   │   ├── EddStatus.java                     ← NEW enum: REQUIRED, SUBMITTED, APPROVED, REJECTED
│   │   │   └── EddTriggerRule.java                ← NEW record
│   │   │
│   │   ├── fraud/                                 ← NEW (Fraud HLD — Core)
│   │   │   ├── FraudEvent.java                    ← NEW record (ingested event from LOS/LMS)
│   │   │   ├── FraudEventType.java                ← NEW enum: ONBOARDING, LOGIN, LOAN_APPLICATION, DISBURSEMENT, REPAYMENT, ACCOUNT_UPDATE, IBAN_CHANGE, PROOF_OF_INDEBTEDNESS
│   │   │   ├── FraudEvaluationResult.java         ← NEW record (output of rule engine)
│   │   │   ├── FraudDecision.java                 ← NEW enum: ALLOW, ALERT, HOLD, BLOCK
│   │   │   ├── FraudBlockType.java                ← NEW enum: TEMPORARY, PERMANENT, SESSION, APPLICATION_LEVEL, DISBURSEMENT_LEVEL, PAYMENT_LEVEL
│   │   │   ├── FraudAlert.java                    ← NEW record (generated alert)
│   │   │   ├── FraudAlertStatus.java              ← NEW enum: OPEN, ASSIGNED, INVESTIGATING, RESOLVED, ESCALATED, DISMISSED
│   │   │   ├── FraudAlertPriority.java            ← NEW enum: CRITICAL, HIGH, MEDIUM, LOW
│   │   │   ├── FraudCase.java                     ← NEW record (case created from alert)
│   │   │   ├── FraudCaseStatus.java               ← NEW enum: OPEN, INVESTIGATING, CONFIRMED_FRAUD, FALSE_POSITIVE, CLOSED
│   │   │   ├── FraudCaseAction.java               ← NEW record (action taken on case)
│   │   │   └── FraudCompositeScore.java           ← NEW record (0-100 composite + triggered rules)
│   │   │
│   │   ├── rule/                                  ← NEW (Fraud HLD — Rule Engine)
│   │   │   ├── FraudRule.java                     ← NEW record (configurable rule definition)
│   │   │   ├── FraudRuleId.java                   ← NEW enum: LOC_001, LOC_002, LOC_003, DEV_001..DEV_003, GEO_001, ACC_001, ACC_002, FIN_001..FIN_003, CARD_001..CARD_003, TMO_001..TMO_017
│   │   │   ├── FraudRuleCategory.java             ← NEW enum: LOCATION, DEVICE, GEOGRAPHIC_ACCESS, FINANCIAL, PAYMENT_CARD, TRANSACTION_MONITORING
│   │   │   ├── FraudRuleStatus.java               ← NEW enum: ACTIVE, DISABLED, TESTING
│   │   │   ├── FraudRuleThreshold.java            ← NEW record (configurable threshold per rule)
│   │   │   ├── RuleEvaluationResult.java          ← NEW record (single rule output: ruleId, triggered, decision, detail, scoreContribution)
│   │   │   └── RuleParameter.java                 ← NEW record (key-value configurable parameter)
│   │   │
│   │   ├── device/                                ← NEW (Fraud HLD — Section 4.1)
│   │   │   ├── DeviceInfo.java                    ← NEW record (type, OS, version, uniqueId, jailbreakStatus, rootStatus)
│   │   │   ├── DeviceType.java                    ← NEW enum: MOBILE, TABLET, DESKTOP
│   │   │   ├── DeviceOS.java                      ← NEW enum: IOS, ANDROID, WINDOWS, MACOS, OTHER
│   │   │   └── DeviceIntegrityStatus.java         ← NEW enum: CLEAN, JAILBROKEN, ROOTED, UNKNOWN
│   │   │
│   │   ├── location/                              ← NEW (Fraud HLD — Section 4.2)
│   │   │   ├── LocationData.java                  ← NEW record (latitude, longitude, country, city, ipAddress, ipGeolocation, isVpn, isProxy)
│   │   │   └── GeoDistance.java                   ← NEW record (distanceKm, fromLocation, toLocation, withinHours)
│   │   │
│   │   ├── session/                               ← NEW (Fraud HLD — Section 4.3)
│   │   │   ├── SessionEvent.java                  ← NEW record (userId, loginTimestamp, deviceId, locationData, sessionId)
│   │   │   └── SessionPattern.java                ← NEW record (loginCount, unusualTimeFlag, lastKnownLocation)
│   │   │
│   │   ├── transaction/                           ← NEW (Fraud HLD — Section 4.5, TMO rules)
│   │   │   ├── TransactionEvent.java              ← NEW record (type, amount, currency, timestamp, customerId, loanId, ibanFrom, ibanTo, cardInfo)
│   │   │   ├── TransactionType.java               ← NEW enum: LOAN_APPLICATION, DISBURSEMENT, REPAYMENT, EARLY_REPAYMENT, IBAN_UPDATE, PROOF_OF_INDEBTEDNESS, ACCOUNT_UPDATE
│   │   │   └── PaymentSource.java                 ← NEW record (iban, cardLast4, cardCountry, cardHolderName, isThirdParty)
│   │   │
│   │   └── blacklist/                             ← NEW (Fraud HLD — Sections 6.2, 6.3, 8)
│   │       ├── DeviceBlacklistEntry.java          ← NEW record (deviceId, reason, blockType, addedAt, escalationCount)
│   │       ├── CountryBlacklistEntry.java         ← NEW record (countryCode, countryName, reason, isActive)
│   │       └── IbanBlacklistEntry.java            ← NEW record (ibanHash, reason, linkedAccountCount)
│   │
│   ├── port/
│   │   ├── in/
│   │   │   ├── RunInternalChecksUseCase.java              ← EXISTING
│   │   │   ├── ManageBlacklistUseCase.java                ← EXISTING
│   │   │   ├── CalculateAmlRiskScoreUseCase.java          ← EXISTING
│   │   │   ├── GetCreditScoringFieldsUseCase.java         ← EXISTING
│   │   │   ├── ManageCreditScoringUseCase.java            ← EXISTING
│   │   │   │
│   │   │   ├── EvaluateFraudEventUseCase.java             ← NEW — Core fraud evaluation (HLD Section 3.3)
│   │   │   ├── ManageFraudRulesUseCase.java               ← NEW — CRUD for 30 fraud rules + thresholds
│   │   │   ├── ManageFraudAlertsUseCase.java              ← NEW — Alert lifecycle (open/assign/resolve/escalate)
│   │   │   ├── ManageFraudCasesUseCase.java               ← NEW — Case management (create/investigate/close)
│   │   │   ├── ManageEddUseCase.java                      ← NEW — EDD trigger + submission + review
│   │   │   ├── ManageDeviceBlacklistUseCase.java          ← NEW — Device blacklist CRUD
│   │   │   ├── ManageCountryBlacklistUseCase.java         ← NEW — Country blacklist CRUD
│   │   │   ├── ManageIbanBlacklistUseCase.java            ← NEW — IBAN blacklist CRUD
│   │   │   ├── GetAmlReferenceDataUseCase.java            ← NEW — Serve KYC/EDD questionnaire options + reference lookups
│   │   │   ├── ManageAmlReferenceDataUseCase.java         ← NEW — Admin CRUD for AML categories/weights/FATF/cities/occupations
│   │   │   ├── ReassessAmlRiskUseCase.java                ← NEW — Periodic AML re-assessment
│   │   │   ├── GetRiskDashboardUseCase.java               ← NEW — Dashboard analytics (risk distribution, alert stats)
│   │   │   └── GetAmlAssessmentHistoryUseCase.java        ← NEW — AML assessment history for compliance reporting
│   │   │
│   │   └── out/
│   │       ├── NidFormatValidator.java                    ← EXISTING
│   │       ├── BlacklistWatchlistCheck.java               ← EXISTING
│   │       ├── FraudHistoryCheck.java                     ← EXISTING
│   │       ├── DeviceFingerprintCheck.java                ← EXISTING
│   │       ├── VelocityCheck.java                         ← EXISTING
│   │       ├── CifLookupCheck.java                        ← EXISTING
│   │       ├── DuplicateMobileCheck.java                  ← EXISTING
│   │       ├── AccountLockCheck.java                      ← EXISTING
│   │       ├── InternalSanctionsCheck.java                ← EXISTING
│   │       ├── RiskScoreCalculator.java                   ← EXISTING
│   │       ├── BlacklistRepository.java                   ← EXISTING
│   │       ├── AmlReferenceDataRepository.java            ← EXISTING
│   │       ├── AmlRiskAssessmentRepository.java           ← EXISTING
│   │       ├── CreditScoringRepository.java               ← EXISTING
│   │       │
│   │       ├── FraudEventRepository.java                  ← NEW — persist fraud events + enrichment data
│   │       ├── FraudRuleRepository.java                   ← NEW — CRUD for rule definitions + thresholds per tenant
│   │       ├── FraudAlertRepository.java                  ← NEW — alert persistence + queries
│   │       ├── FraudCaseRepository.java                   ← NEW — case persistence + queries
│   │       ├── EddRepository.java                         ← NEW — EDD requirements + submissions
│   │       ├── DeviceBlacklistRepository.java             ← NEW — device blacklist persistence
│   │       ├── CountryBlacklistRepository.java            ← NEW — country blacklist persistence
│   │       ├── IbanBlacklistRepository.java               ← NEW — IBAN blacklist persistence
│   │       ├── SessionEventRepository.java                ← NEW — session/login event persistence
│   │       ├── TransactionEventRepository.java            ← NEW — transaction event persistence
│   │       ├── LocationDataRepository.java                ← NEW — location history persistence
│   │       ├── FraudUserProfileRepository.java            ← NEW — mirrored user profile for fraud
│   │       └── FraudNotificationPort.java                 ← NEW — send alerts to notification-service / dashboard
│   │
│   └── service/
│       ├── AmlRiskScoringEngine.java                      ← EXISTING (pure domain service)
│       ├── FraudRuleEngine.java                           ← NEW — evaluates all active rules against enriched event
│       ├── FraudDecisionEngine.java                       ← NEW — consolidates rule outputs → final decision (ALLOW/ALERT/HOLD/BLOCK)
│       ├── GeoDistanceCalculator.java                     ← NEW — Haversine distance between two GPS coordinates
│       └── EddTriggerEngine.java                          ← NEW — determines if EDD is required based on AML score
│
├── application/
│   ├── dto/
│   │   ├── StartInternalCheckRequestDto.java              ← EXISTING
│   │   ├── AmlRiskScoreRequestDto.java                    ← EXISTING
│   │   ├── AmlRiskScoreResponseDto.java                   ← EXISTING
│   │   │
│   │   ├── fraud/                                         ← NEW
│   │   │   ├── FraudEventRequestDto.java                  ← NEW — ingest event from LOS/LMS
│   │   │   ├── FraudEvaluationResponseDto.java            ← NEW — return decision to caller
│   │   │   ├── FraudAlertResponseDto.java                 ← NEW
│   │   │   ├── FraudCaseResponseDto.java                  ← NEW
│   │   │   ├── FraudRuleConfigDto.java                    ← NEW — rule definition + thresholds for admin
│   │   │   ├── UpdateFraudRuleRequestDto.java             ← NEW — update rule thresholds
│   │   │   ├── AssignAlertRequestDto.java                 ← NEW
│   │   │   ├── ResolveAlertRequestDto.java                ← NEW
│   │   │   ├── UpdateCaseRequestDto.java                  ← NEW
│   │   │   └── FraudDashboardResponseDto.java             ← NEW — dashboard aggregation
│   │   │
│   │   ├── edd/                                           ← NEW
│   │   │   ├── EddRequirementResponseDto.java             ← NEW
│   │   │   ├── EddSubmissionRequestDto.java               ← NEW
│   │   │   ├── EddSubmissionResponseDto.java              ← NEW
│   │   │   └── EddReviewRequestDto.java                   ← NEW
│   │   │
│   │   ├── aml/                                           ← NEW
│   │   │   ├── AmlCategoryConfigDto.java                  ← NEW — admin weight config
│   │   │   ├── AmlThresholdConfigDto.java                 ← NEW — admin threshold config
│   │   │   ├── KycQuestionnaireResponseDto.java           ← NEW — serve KYC options (bilingual)
│   │   │   ├── AmlAssessmentHistoryResponseDto.java       ← NEW
│   │   │   └── AmlDashboardResponseDto.java               ← NEW
│   │   │
│   │   └── blacklist/                                     ← NEW
│   │       ├── DeviceBlacklistRequestDto.java             ← NEW
│   │       ├── CountryBlacklistRequestDto.java            ← NEW
│   │       └── IbanBlacklistRequestDto.java               ← NEW
│   │
│   ├── state/
│   │   └── InternalCheckState.java                        ← EXISTING
│   │
│   ├── usecase/
│   │   ├── RunInternalChecksService.java                  ← EXISTING
│   │   ├── CalculateAmlRiskScoreService.java              ← EXISTING
│   │   ├── ManageBlacklistUseCaseImpl.java                ← EXISTING
│   │   ├── GetCreditScoringFieldsService.java             ← EXISTING
│   │   ├── ManageCreditScoringService.java                ← EXISTING
│   │   │
│   │   ├── EvaluateFraudEventService.java                 ← NEW — core fraud evaluation orchestrator
│   │   ├── ManageFraudRulesService.java                   ← NEW
│   │   ├── ManageFraudAlertsService.java                  ← NEW
│   │   ├── ManageFraudCasesService.java                   ← NEW
│   │   ├── ManageEddService.java                          ← NEW
│   │   ├── ManageDeviceBlacklistService.java              ← NEW
│   │   ├── ManageCountryBlacklistService.java             ← NEW
│   │   ├── ManageIbanBlacklistService.java                ← NEW
│   │   ├── GetAmlReferenceDataService.java                ← NEW
│   │   ├── ManageAmlReferenceDataService.java             ← NEW
│   │   ├── ReassessAmlRiskService.java                    ← NEW
│   │   ├── GetRiskDashboardService.java                   ← NEW
│   │   └── GetAmlAssessmentHistoryService.java            ← NEW
│   │
│   └── mapper/
│       ├── FraudEventMapper.java                          ← NEW
│       └── EddMapper.java                                 ← NEW
│
├── infrastructure/
│   ├── check/
│   │   ├── NidFormatValidatorImpl.java                    ← EXISTING
│   │   ├── BlacklistWatchlistCheckImpl.java               ← EXISTING
│   │   ├── FraudHistoryCheckImpl.java                     ← EXISTING
│   │   ├── DeviceFingerprintCheckImpl.java                ← EXISTING
│   │   ├── VelocityCheckImpl.java                         ← EXISTING
│   │   ├── CifLookupCheckImpl.java                        ← EXISTING
│   │   ├── DuplicateMobileCheckImpl.java                  ← EXISTING
│   │   ├── AccountLockCheckImpl.java                      ← EXISTING
│   │   ├── InternalSanctionsCheckImpl.java                ← EXISTING
│   │   └── RiskScoreCalculatorImpl.java                   ← EXISTING
│   │
│   ├── rule/                                              ← NEW — Fraud rule implementations (HLD Section 5)
│   │   ├── location/
│   │   │   ├── UnusualLocationChangeRule.java             ← NEW (LOC-001)
│   │   │   ├── SuddenLocationChangeSameDeviceRule.java    ← NEW (LOC-002)
│   │   │   └── NationalAddressMismatchRule.java           ← NEW (LOC-003)
│   │   │
│   │   ├── device/
│   │   │   ├── MultipleAccountsSameDeviceRule.java        ← NEW (DEV-001)
│   │   │   ├── MultipleLoanAppsSameDeviceRule.java        ← NEW (DEV-002)
│   │   │   └── BlacklistedDeviceRule.java                 ← NEW (DEV-003)
│   │   │
│   │   ├── access/
│   │   │   ├── BlacklistedCountryAccessRule.java          ← NEW (GEO-001)
│   │   │   ├── VpnProxyDetectedRule.java                  ← NEW (ACC-001)
│   │   │   └── JailbrokenRootedDeviceRule.java            ← NEW (ACC-002)
│   │   │
│   │   ├── financial/
│   │   │   ├── MultipleAccountsSameIbanRule.java          ← NEW (FIN-001)
│   │   │   ├── LoanAmountTransferMismatchRule.java        ← NEW (FIN-002)
│   │   │   └── ExcessiveLoanApplicationsRule.java         ← NEW (FIN-003)
│   │   │
│   │   ├── card/
│   │   │   ├── InternationalCardInstallmentRule.java      ← NEW (CARD-001)
│   │   │   ├── FrequentCardChangesRule.java               ← NEW (CARD-002)
│   │   │   └── CardholderNameMismatchRule.java            ← NEW (CARD-003)
│   │   │
│   │   └── transaction/
│   │       ├── HighFrequencyLoanApplicationsRule.java     ← NEW (TMO-001)
│   │       ├── ApplicationRateLimitBreachRule.java        ← NEW (TMO-002)
│   │       ├── DuplicateLoanApplicationRule.java          ← NEW (TMO-003)
│   │       ├── LateIbanSubstitutionRule.java              ← NEW (TMO-004)
│   │       ├── MultipleDisbursementsSameIbanRule.java     ← NEW (TMO-005)
│   │       ├── DisbursementAmountMismatchRule.java        ← NEW (TMO-006)
│   │       ├── DisbursementUnverifiedIbanRule.java        ← NEW (TMO-007)
│   │       ├── SameDayFullRepaymentRule.java              ← NEW (TMO-008)
│   │       ├── ThirdPartyPaymentRule.java                 ← NEW (TMO-009)
│   │       ├── RepeatedPaymentReversalRule.java           ← NEW (TMO-010)
│   │       ├── InstallmentAmountDiscrepancyRule.java      ← NEW (TMO-011)
│   │       ├── DormantAccountActivityRule.java            ← NEW (TMO-012)
│   │       ├── UnusualTransactionTimeRule.java            ← NEW (TMO-013)
│   │       ├── RoundNumberPatternRule.java                ← NEW (TMO-014)
│   │       ├── HighVolumeProofOfIndebtednessRule.java     ← NEW (TMO-015)
│   │       ├── EarlyRepaymentReApplicationRule.java       ← NEW (TMO-016)
│   │       └── CoordinatedApplicationSpikeRule.java       ← NEW (TMO-017)
│   │
│   ├── enrichment/                                        ← NEW — Event enrichment pipeline (HLD Section 3.3 Step 2)
│   │   ├── IpGeolocationEnricher.java                     ← NEW — IP → country/city/coordinates
│   │   ├── VpnProxyDetector.java                          ← NEW — detect VPN/proxy from IP
│   │   ├── DeviceIntegrityEnricher.java                   ← NEW — jailbreak/root status enrichment
│   │   └── DistanceCalculationEnricher.java               ← NEW — calculate distance from last known location
│   │
│   ├── persistence/
│   │   ├── BlacklistRepositoryImpl.java                   ← EXISTING
│   │   ├── AmlReferenceDataRepositoryImpl.java            ← EXISTING
│   │   ├── AmlRiskAssessmentRepositoryImpl.java           ← EXISTING
│   │   ├── CreditScoringRepositoryImpl.java               ← EXISTING
│   │   │
│   │   ├── FraudEventRepositoryImpl.java                  ← NEW
│   │   ├── FraudRuleRepositoryImpl.java                   ← NEW
│   │   ├── FraudAlertRepositoryImpl.java                  ← NEW
│   │   ├── FraudCaseRepositoryImpl.java                   ← NEW
│   │   ├── EddRepositoryImpl.java                         ← NEW
│   │   ├── DeviceBlacklistRepositoryImpl.java             ← NEW
│   │   ├── CountryBlacklistRepositoryImpl.java            ← NEW
│   │   ├── IbanBlacklistRepositoryImpl.java               ← NEW
│   │   ├── SessionEventRepositoryImpl.java                ← NEW
│   │   ├── TransactionEventRepositoryImpl.java            ← NEW
│   │   ├── LocationDataRepositoryImpl.java                ← NEW
│   │   └── FraudUserProfileRepositoryImpl.java            ← NEW
│   │
│   ├── messaging/
│   │   ├── FraudEventKafkaConsumer.java                   ← NEW — consume events from LOS/LMS
│   │   ├── FraudAlertKafkaPublisher.java                  ← NEW — publish alerts
│   │   └── FraudNotificationAdapter.java                  ← NEW — send notifications
│   │
│   └── config/
│       ├── SecurityConfig.java                            ← EXISTING (will EXTEND, not replace)
│       ├── DomainServiceConfig.java                       ← EXISTING (will EXTEND)
│       ├── RestTemplateConfig.java                        ← EXISTING
│       ├── FraudRuleEngineConfig.java                     ← NEW — register all 30 rules as beans
│       └── KafkaConsumerConfig.java                       ← NEW — Kafka consumer configuration
│
├── adapter/
│   └── rest/
│       ├── controller/
│       │   ├── InternalChecksController.java              ← EXISTING
│       │   ├── AmlRiskScoreController.java                ← EXISTING
│       │   ├── BlacklistController.java                   ← EXISTING
│       │   ├── CreditScoringController.java               ← EXISTING
│       │   │
│       │   ├── FraudEvaluationController.java             ← NEW — POST /api/v1/risk/fraud/evaluate
│       │   ├── FraudRuleController.java                   ← NEW — CRUD /api/v1/risk/fraud/rules
│       │   ├── FraudAlertController.java                  ← NEW — /api/v1/risk/fraud/alerts
│       │   ├── FraudCaseController.java                   ← NEW — /api/v1/risk/fraud/cases
│       │   ├── EddController.java                         ← NEW — /api/v1/risk/edd
│       │   ├── DeviceBlacklistController.java             ← NEW — /api/v1/risk/blacklist/devices
│       │   ├── CountryBlacklistController.java            ← NEW — /api/v1/risk/blacklist/countries
│       │   ├── IbanBlacklistController.java               ← NEW — /api/v1/risk/blacklist/ibans
│       │   ├── AmlReferenceDataController.java            ← NEW — /api/v1/risk/aml/reference-data
│       │   ├── AmlAssessmentHistoryController.java        ← NEW — /api/v1/risk/aml/assessments
│       │   └── RiskDashboardController.java               ← NEW — /api/v1/risk/dashboard
│       │
│       └── request/
│           ├── BlacklistNidRequest.java                   ← EXISTING
│           ├── BlacklistMobileRequest.java                ← EXISTING
│           ├── SaveCreditScoringRequest.java              ← EXISTING
│           │
│           ├── BlacklistDeviceRequest.java                ← NEW
│           ├── BlacklistCountryRequest.java               ← NEW
│           └── BlacklistIbanRequest.java                  ← NEW
│
└── RiskServiceApplication.java                            ← EXISTING
```

---

## 3. NEW DOMAIN MODELS — DETAILED DESIGN

### 3.1 Fraud Event (HLD Section 3.3 — Ingested from LOS/LMS)

```java
// The central event record — every interaction triggers this
public record FraudEvent(
    UUID id,
    UUID tenantId,
    String eventId,                    // idempotency
    FraudEventType eventType,          // ONBOARDING, LOGIN, LOAN_APPLICATION, DISBURSEMENT, REPAYMENT, etc.
    String customerId,                 // NID hash or customer UUID
    String nationalIdHash,
    // Device Data (HLD Table 3)
    DeviceInfo deviceInfo,
    // Location Data (HLD Table 4)
    LocationData locationData,
    // Session Data (HLD Table 5)
    LocalDateTime eventTimestamp,
    String sessionId,
    // Transaction Data (HLD Table 7) — null for non-financial events
    TransactionType transactionType,
    BigDecimal transactionAmount,
    String currency,
    // Loan-specific
    String loanApplicationId,
    String loanProductType,
    BigDecimal approvedLoanAmount,
    // IBAN-specific
    String disbursementIban,
    String ibanVerificationStatus,     // VERIFIED, UNVERIFIED
    String ibanHolderName,
    // Payment-specific
    PaymentSource paymentSource,
    // Enrichment (populated by enrichment pipeline)
    String resolvedCountry,
    String resolvedCity,
    BigDecimal distanceFromLastKm,
    Duration timeSinceLastLogin,
    boolean vpnDetected,
    boolean proxyDetected,
    // Metadata
    LocalDateTime receivedAt,
    String correlationId
) {}
```

### 3.2 Fraud Rule (HLD Section 5 — Configurable)

```java
// Each of the 30 rules is represented by this record in the DB
public record FraudRule(
    UUID id,
    UUID tenantId,
    FraudRuleId ruleId,                // LOC_001, DEV_001, TMO_001, etc.
    String scenarioName,               // "Unusual Location Change"
    String scenarioNameAr,             // Arabic name
    FraudRuleCategory category,        // LOCATION, DEVICE, TRANSACTION_MONITORING, etc.
    String detectionLogic,             // Human-readable description
    FraudDecision defaultAction,       // ALERT, HOLD, BLOCK
    FraudBlockType blockType,          // TEMPORARY, PERMANENT, SESSION, etc.
    FraudRuleStatus status,            // ACTIVE, DISABLED, TESTING
    List<RuleParameter> parameters,    // Configurable thresholds (JSON)
    int priority,                      // Evaluation order
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
```

### 3.3 Fraud Rule Parameters (Configurable per HLD — "SHALL NOT be hardcoded")

```java
// Every threshold is a key-value pair — admin-configurable per tenant
public record RuleParameter(
    String key,            // e.g., "distance_km", "time_window_hours", "max_accounts"
    String value,          // e.g., "200", "3", "48"
    String dataType,       // INT, DECIMAL, DURATION_HOURS, DURATION_DAYS, BOOLEAN
    String description,    // Human-readable
    String descriptionAr   // Arabic
) {}

// Example parameters per rule (from HLD):
// LOC-001: distance_km=200, time_window_hours=3, block_duration_hours=72
// DEV-001: max_accounts=3, time_window_hours=48, escalate_on_repeat=true
// TMO-001: max_applications=3, rolling_window_hours=24
// TMO-002: max_per_month=2, max_per_year=4
// TMO-004: min_hours_before_disbursement=24
// TMO-008: repayment_window_hours=48
// TMO-012: dormant_days=90
// TMO-013: unusual_hour_start=01, unusual_hour_end=04
// TMO-014: round_denomination=500, min_consecutive=3
// TMO-017: spike_multiplier=3.0, rolling_window_hours=2, baseline_days=30
```

### 3.4 Fraud Evaluation Result (HLD Section 3.3 Step 4-6)

```java
// The synchronous response returned to LOS/LMS
public record FraudEvaluationResult(
    UUID evaluationId,
    UUID tenantId,
    String eventId,
    FraudDecision decision,            // ALLOW, ALERT, HOLD, BLOCK
    FraudBlockType blockType,          // null if ALLOW
    int compositeRiskScore,            // 0-100
    RiskLevel riskLevel,               // LOW, MEDIUM, HIGH, CRITICAL
    List<RuleEvaluationResult> triggeredRules,   // only rules that fired
    String blockReason,                // null if not blocked
    Duration blockDuration,            // null if not blocked
    String customerMessage,            // safe message to show customer (no fraud details)
    LocalDateTime evaluatedAt,
    long evaluationTimeMs              // must be < 500ms per HLD Section 7
) {}
```

### 3.5 Fraud Alert (HLD Section 3.3 Step 5-8)

```java
public record FraudAlert(
    UUID id,
    UUID tenantId,
    UUID evaluationId,                 // links to FraudEvaluationResult
    String customerId,
    FraudRuleId triggeringRuleId,       // which rule triggered this alert
    FraudAlertPriority priority,        // CRITICAL, HIGH, MEDIUM, LOW
    FraudAlertStatus status,            // OPEN, ASSIGNED, INVESTIGATING, RESOLVED, ESCALATED, DISMISSED
    FraudDecision decision,
    String summary,                    // "Multiple accounts on same device detected"
    String summaryAr,                  // Arabic
    JSONB details,                     // full event + rule data
    UUID assignedTo,                   // fraud analyst UUID
    LocalDateTime assignedAt,
    UUID resolvedBy,
    LocalDateTime resolvedAt,
    String resolutionNote,
    LocalDateTime createdAt,
    LocalDateTime slaDeadline          // createdAt + 60 seconds (HLD Section 7)
) {}
```

### 3.6 Fraud Case (HLD Section 3.2 — Case Management)

```java
public record FraudCase(
    UUID id,
    UUID tenantId,
    String caseNumber,                 // auto-generated: FRD-2026-000001
    String customerId,
    FraudCaseStatus status,            // OPEN, INVESTIGATING, CONFIRMED_FRAUD, FALSE_POSITIVE, CLOSED
    List<UUID> linkedAlertIds,         // alerts that belong to this case
    List<FraudCaseAction> actions,     // audit trail of actions taken
    UUID assignedTo,
    String summary,
    BigDecimal estimatedLoss,
    LocalDateTime createdAt,
    LocalDateTime closedAt,
    String closureNote
) {}

public record FraudCaseAction(
    UUID id,
    UUID caseId,
    String action,                     // "ACCOUNT_BLOCKED", "CUSTOMER_CONTACTED", "ESCALATED_TO_COMPLIANCE", etc.
    String note,
    UUID performedBy,
    LocalDateTime performedAt
) {}
```

### 3.7 EDD Models (AML Excel — EDD Sheet)

```java
// EDD is triggered when AML score >= HIGH (42+)
public record EddRequirement(
    UUID id,
    UUID tenantId,
    String customerId,
    UUID amlAssessmentId,              // links to the AML score that triggered EDD
    AmlRiskLevel triggeringRiskLevel,
    int triggeringScore,
    EddStatus status,                  // REQUIRED, SUBMITTED, APPROVED, REJECTED
    List<EddQuestion> questions,       // loaded from edd_questions table
    LocalDateTime requiredAt,
    LocalDateTime submittedAt,
    LocalDateTime reviewedAt,
    UUID reviewedBy,
    String reviewNote
) {}

public record EddQuestion(
    UUID id,
    UUID tenantId,
    String questionCode,               // SOURCE_OF_INCOME, SOURCE_OF_WEALTH, WEALTH_VALUE
    String questionEn,                 // "Source of income"
    String questionAr,                 // "مصدر الدخل"
    String answerType,                 // SINGLE_SELECT, FREE_TEXT, RANGE_SELECT
    List<EddAnswer> options,           // predefined answer options
    boolean mandatory,
    int sortOrder
) {}

public record EddAnswer(
    UUID id,
    UUID questionId,
    String answerCode,                 // SALARY, FAMILY_SUPPORT, REAL_ESTATE, BUSINESS, ADVANCE, INVESTMENT
    String answerEn,                   // "Salary"
    String answerAr,                   // "راتب"
    int sortOrder
) {}

public record EddSubmission(
    UUID id,
    UUID eddRequirementId,
    UUID questionId,
    String answerCode,                 // selected option code or null for free text
    String freeTextAnswer,             // for free text questions
    LocalDateTime submittedAt
) {}
```

### 3.8 Device Info (HLD Table 3)

```java
public record DeviceInfo(
    String deviceId,                   // unique persistent device identifier
    DeviceType deviceType,             // MOBILE, TABLET, DESKTOP
    DeviceOS operatingSystem,          // IOS, ANDROID, WINDOWS, MACOS
    String osVersion,                  // "17.2.1"
    String deviceFingerprint,          // browser/app fingerprint
    DeviceIntegrityStatus integrityStatus,  // CLEAN, JAILBROKEN, ROOTED, UNKNOWN
    String appVersion                  // Finova app version
) {}
```

### 3.9 Location Data (HLD Table 4)

```java
public record LocationData(
    BigDecimal latitude,               // GPS
    BigDecimal longitude,              // GPS
    String ipAddress,
    String ipCountry,                  // derived from IP geolocation
    String ipCity,                     // derived from IP geolocation
    String gpsCountry,                 // derived from GPS
    String gpsCity,                    // derived from GPS
    boolean vpnDetected,
    boolean proxyDetected
) {}
```

---

## 4. NEW DATABASE MIGRATIONS

### V8 — Fraud Monitoring Core Schema

```sql
-- V8__fraud_monitoring_schema.sql

-- ============================================================================
-- FRAUD MONITORING SYSTEM (Finova Fraud HLD v1)
-- ============================================================================

-- ENUMS
CREATE TYPE fraud_event_type AS ENUM (
    'ONBOARDING', 'LOGIN', 'LOAN_APPLICATION', 'DISBURSEMENT',
    'REPAYMENT', 'EARLY_REPAYMENT', 'IBAN_UPDATE', 'PROOF_OF_INDEBTEDNESS',
    'ACCOUNT_UPDATE'
);

CREATE TYPE fraud_decision AS ENUM ('ALLOW', 'ALERT', 'HOLD', 'BLOCK');

CREATE TYPE fraud_block_type AS ENUM (
    'TEMPORARY', 'PERMANENT', 'SESSION',
    'APPLICATION_LEVEL', 'DISBURSEMENT_LEVEL', 'PAYMENT_LEVEL'
);

CREATE TYPE fraud_alert_status AS ENUM (
    'OPEN', 'ASSIGNED', 'INVESTIGATING', 'RESOLVED', 'ESCALATED', 'DISMISSED'
);

CREATE TYPE fraud_alert_priority AS ENUM ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW');

CREATE TYPE fraud_case_status AS ENUM (
    'OPEN', 'INVESTIGATING', 'CONFIRMED_FRAUD', 'FALSE_POSITIVE', 'CLOSED'
);

CREATE TYPE fraud_rule_status AS ENUM ('ACTIVE', 'DISABLED', 'TESTING');

CREATE TYPE fraud_rule_category AS ENUM (
    'LOCATION', 'DEVICE', 'GEOGRAPHIC_ACCESS',
    'FINANCIAL', 'PAYMENT_CARD', 'TRANSACTION_MONITORING'
);

CREATE TYPE device_type AS ENUM ('MOBILE', 'TABLET', 'DESKTOP');
CREATE TYPE device_os AS ENUM ('IOS', 'ANDROID', 'WINDOWS', 'MACOS', 'OTHER');
CREATE TYPE device_integrity AS ENUM ('CLEAN', 'JAILBROKEN', 'ROOTED', 'UNKNOWN');

-- ============================================================================
-- FRAUD RULES (configurable per tenant — HLD Section 5)
-- ============================================================================

CREATE TABLE fraud_rules (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    rule_id         VARCHAR(20) NOT NULL,           -- LOC_001, DEV_001, TMO_001, etc.
    scenario_name   VARCHAR(200) NOT NULL,
    scenario_name_ar VARCHAR(200),
    category        fraud_rule_category NOT NULL,
    detection_logic TEXT,
    default_action  fraud_decision NOT NULL,
    block_type      fraud_block_type,
    status          fraud_rule_status NOT NULL DEFAULT 'ACTIVE',
    parameters      JSONB NOT NULL DEFAULT '[]',    -- configurable thresholds
    priority        INT NOT NULL DEFAULT 100,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         INT NOT NULL DEFAULT 1,
    CONSTRAINT uq_fraud_rule_tenant UNIQUE (tenant_id, rule_id)
);

CREATE INDEX idx_fraud_rules_tenant ON fraud_rules(tenant_id);
CREATE INDEX idx_fraud_rules_active ON fraud_rules(tenant_id) WHERE status = 'ACTIVE';

-- ============================================================================
-- FRAUD EVENTS (ingested from LOS/LMS — HLD Section 4)
-- ============================================================================

CREATE TABLE fraud_events (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    event_id            VARCHAR(100) NOT NULL,      -- idempotency key from caller
    event_type          fraud_event_type NOT NULL,
    customer_id         VARCHAR(255) NOT NULL,       -- NID hash or UUID
    national_id_hash    VARCHAR(255),
    -- Device Data (HLD Table 3)
    device_id           VARCHAR(255),
    device_type         device_type,
    device_os           device_os,
    os_version          VARCHAR(50),
    device_fingerprint  VARCHAR(512),
    device_integrity    device_integrity,
    -- Location Data (HLD Table 4)
    latitude            NUMERIC(10, 7),
    longitude           NUMERIC(10, 7),
    ip_address          VARCHAR(45),                 -- IPv4 or IPv6
    ip_country          VARCHAR(3),                  -- ISO country code
    ip_city             VARCHAR(200),
    gps_country         VARCHAR(3),
    gps_city            VARCHAR(200),
    vpn_detected        BOOLEAN DEFAULT FALSE,
    proxy_detected      BOOLEAN DEFAULT FALSE,
    -- Session Data (HLD Table 5)
    session_id          VARCHAR(255),
    event_timestamp     TIMESTAMPTZ NOT NULL,
    -- Transaction Data (HLD Table 7)
    transaction_type    VARCHAR(50),
    transaction_amount  NUMERIC(20, 4),
    currency            VARCHAR(3) DEFAULT 'SAR',
    -- Loan-specific
    loan_application_id VARCHAR(100),
    loan_product_type   VARCHAR(100),
    approved_loan_amount NUMERIC(20, 4),
    -- IBAN-specific
    disbursement_iban   VARCHAR(34),
    iban_verification_status VARCHAR(20),
    iban_holder_name    VARCHAR(255),
    -- Payment-specific
    payment_iban        VARCHAR(34),
    card_last4          VARCHAR(4),
    card_country        VARCHAR(3),
    card_holder_name    VARCHAR(255),
    is_third_party_payment BOOLEAN DEFAULT FALSE,
    -- Enrichment (populated by enrichment pipeline)
    resolved_country    VARCHAR(3),
    resolved_city       VARCHAR(200),
    distance_from_last_km NUMERIC(10, 2),
    time_since_last_login_minutes INT,
    -- Metadata
    correlation_id      VARCHAR(100),
    received_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_fraud_event UNIQUE (tenant_id, event_id)
);

CREATE INDEX idx_fraud_events_tenant ON fraud_events(tenant_id);
CREATE INDEX idx_fraud_events_customer ON fraud_events(tenant_id, customer_id);
CREATE INDEX idx_fraud_events_device ON fraud_events(device_id);
CREATE INDEX idx_fraud_events_type ON fraud_events(tenant_id, event_type);
CREATE INDEX idx_fraud_events_iban ON fraud_events(disbursement_iban) WHERE disbursement_iban IS NOT NULL;
CREATE INDEX idx_fraud_events_timestamp ON fraud_events(tenant_id, event_timestamp);

-- ============================================================================
-- FRAUD EVALUATIONS (rule engine output — HLD Section 3.3 Step 4-5)
-- ============================================================================

CREATE TABLE fraud_evaluations (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    event_id            VARCHAR(100) NOT NULL,
    fraud_event_id      UUID NOT NULL REFERENCES fraud_events(id),
    customer_id         VARCHAR(255) NOT NULL,
    decision            fraud_decision NOT NULL,
    block_type          fraud_block_type,
    composite_risk_score INT NOT NULL,
    risk_level          VARCHAR(20) NOT NULL,
    triggered_rules     JSONB NOT NULL DEFAULT '[]', -- array of {ruleId, decision, detail, scoreContribution}
    block_reason        TEXT,
    block_duration_hours INT,
    customer_message    TEXT,
    evaluation_time_ms  INT NOT NULL,
    evaluated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_fraud_evaluation UNIQUE (tenant_id, event_id)
);

CREATE INDEX idx_fraud_eval_tenant ON fraud_evaluations(tenant_id);
CREATE INDEX idx_fraud_eval_customer ON fraud_evaluations(tenant_id, customer_id);
CREATE INDEX idx_fraud_eval_decision ON fraud_evaluations(tenant_id, decision);

-- ============================================================================
-- FRAUD ALERTS (HLD Section 3.3 Step 5-8)
-- ============================================================================

CREATE TABLE fraud_alerts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    evaluation_id   UUID NOT NULL REFERENCES fraud_evaluations(id),
    customer_id     VARCHAR(255) NOT NULL,
    triggering_rule_id VARCHAR(20) NOT NULL,
    priority        fraud_alert_priority NOT NULL,
    status          fraud_alert_status NOT NULL DEFAULT 'OPEN',
    decision        fraud_decision NOT NULL,
    summary         TEXT NOT NULL,
    summary_ar      TEXT,
    details         JSONB,
    assigned_to     UUID,
    assigned_at     TIMESTAMPTZ,
    resolved_by     UUID,
    resolved_at     TIMESTAMPTZ,
    resolution_note TEXT,
    sla_deadline    TIMESTAMPTZ NOT NULL,          -- created_at + 60 seconds (HLD)
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         INT NOT NULL DEFAULT 1
);

CREATE INDEX idx_fraud_alerts_tenant ON fraud_alerts(tenant_id);
CREATE INDEX idx_fraud_alerts_status ON fraud_alerts(tenant_id, status);
CREATE INDEX idx_fraud_alerts_priority ON fraud_alerts(tenant_id, priority);
CREATE INDEX idx_fraud_alerts_assigned ON fraud_alerts(assigned_to) WHERE assigned_to IS NOT NULL;
CREATE INDEX idx_fraud_alerts_open ON fraud_alerts(tenant_id) WHERE status = 'OPEN';
CREATE INDEX idx_fraud_alerts_sla ON fraud_alerts(sla_deadline) WHERE status IN ('OPEN', 'ASSIGNED');

-- ============================================================================
-- FRAUD CASES (HLD Section 3.2 — Case Management)
-- ============================================================================

CREATE TABLE fraud_cases (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    case_number     VARCHAR(50) NOT NULL,           -- FRD-2026-000001
    customer_id     VARCHAR(255) NOT NULL,
    status          fraud_case_status NOT NULL DEFAULT 'OPEN',
    assigned_to     UUID,
    summary         TEXT,
    estimated_loss  NUMERIC(20, 4),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    closed_at       TIMESTAMPTZ,
    closure_note    TEXT,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         INT NOT NULL DEFAULT 1,
    CONSTRAINT uq_fraud_case_number UNIQUE (tenant_id, case_number)
);

CREATE TABLE fraud_case_alerts (
    case_id         UUID NOT NULL REFERENCES fraud_cases(id),
    alert_id        UUID NOT NULL REFERENCES fraud_alerts(id),
    linked_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (case_id, alert_id)
);

CREATE TABLE fraud_case_actions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    case_id         UUID NOT NULL REFERENCES fraud_cases(id),
    action          VARCHAR(100) NOT NULL,
    note            TEXT,
    performed_by    UUID NOT NULL,
    performed_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fraud_cases_tenant ON fraud_cases(tenant_id);
CREATE INDEX idx_fraud_cases_status ON fraud_cases(tenant_id, status);
CREATE INDEX idx_fraud_cases_customer ON fraud_cases(tenant_id, customer_id);

-- ============================================================================
-- SESSION HISTORY (HLD Section 4.3 — Login & Session Timing)
-- ============================================================================

CREATE TABLE session_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    customer_id     VARCHAR(255) NOT NULL,
    session_id      VARCHAR(255),
    device_id       VARCHAR(255),
    ip_address      VARCHAR(45),
    latitude        NUMERIC(10, 7),
    longitude       NUMERIC(10, 7),
    country         VARCHAR(3),
    city            VARCHAR(200),
    login_at        TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_session_customer ON session_events(tenant_id, customer_id);
CREATE INDEX idx_session_device ON session_events(device_id);
CREATE INDEX idx_session_time ON session_events(tenant_id, customer_id, login_at);

-- ============================================================================
-- FRAUD USER PROFILES (HLD Section 4.4 — Mirrored KYC data)
-- ============================================================================

CREATE TABLE fraud_user_profiles (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    customer_id         VARCHAR(255) NOT NULL,
    national_id_hash    VARCHAR(255) NOT NULL,
    full_name_hash      VARCHAR(255),
    nationality         VARCHAR(3),
    mobile_hash         VARCHAR(255),
    email_hash          VARCHAR(255),
    registered_address_city VARCHAR(200),
    registered_address_country VARCHAR(3),
    registered_iban     VARCHAR(34),
    iban_verified       BOOLEAN DEFAULT FALSE,
    iban_holder_name    VARCHAR(255),
    aml_risk_level      VARCHAR(20),
    last_login_at       TIMESTAMPTZ,
    last_login_latitude NUMERIC(10, 7),
    last_login_longitude NUMERIC(10, 7),
    last_login_country  VARCHAR(3),
    last_login_city     VARCHAR(200),
    last_login_device_id VARCHAR(255),
    account_created_at  TIMESTAMPTZ,
    last_activity_at    TIMESTAMPTZ,
    total_loan_applications INT DEFAULT 0,
    total_disbursements INT DEFAULT 0,
    total_repayments    INT DEFAULT 0,
    is_dormant          BOOLEAN DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_fraud_user_profile UNIQUE (tenant_id, customer_id)
);

CREATE INDEX idx_fraud_user_customer ON fraud_user_profiles(tenant_id, customer_id);
CREATE INDEX idx_fraud_user_nid ON fraud_user_profiles(national_id_hash);
CREATE INDEX idx_fraud_user_iban ON fraud_user_profiles(registered_iban) WHERE registered_iban IS NOT NULL;
CREATE INDEX idx_fraud_user_dormant ON fraud_user_profiles(tenant_id) WHERE is_dormant = TRUE;

-- ============================================================================
-- EXTENDED BLACKLISTS (HLD Sections 6.2, 6.3, 8)
-- ============================================================================

CREATE TABLE device_blacklist (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    device_id       VARCHAR(255) NOT NULL,
    reason          TEXT NOT NULL,
    block_type      fraud_block_type NOT NULL DEFAULT 'TEMPORARY',
    escalation_count INT NOT NULL DEFAULT 0,         -- DEV-001: escalates to PERMANENT on second occurrence
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    added_by        UUID,
    added_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_device_blacklist UNIQUE (tenant_id, device_id)
);

CREATE TABLE country_blacklist (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    country_code    VARCHAR(3) NOT NULL,             -- ISO 3166-1 alpha-2/alpha-3
    country_name    VARCHAR(200) NOT NULL,
    country_name_ar VARCHAR(200),
    reason          TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    added_by        UUID,
    added_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_country_blacklist UNIQUE (tenant_id, country_code)
);

CREATE TABLE iban_blacklist (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    iban_hash       VARCHAR(255) NOT NULL,
    reason          TEXT NOT NULL,
    linked_account_count INT DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    added_by        UUID,
    added_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_iban_blacklist UNIQUE (tenant_id, iban_hash)
);

CREATE INDEX idx_device_bl_active ON device_blacklist(tenant_id) WHERE is_active = TRUE;
CREATE INDEX idx_country_bl_active ON country_blacklist(tenant_id) WHERE is_active = TRUE;
CREATE INDEX idx_iban_bl_active ON iban_blacklist(tenant_id) WHERE is_active = TRUE;

-- ============================================================================
-- ROW LEVEL SECURITY
-- ============================================================================

ALTER TABLE fraud_rules ENABLE ROW LEVEL SECURITY;
ALTER TABLE fraud_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE fraud_evaluations ENABLE ROW LEVEL SECURITY;
ALTER TABLE fraud_alerts ENABLE ROW LEVEL SECURITY;
ALTER TABLE fraud_cases ENABLE ROW LEVEL SECURITY;
ALTER TABLE fraud_user_profiles ENABLE ROW LEVEL SECURITY;
```

### V9 — Fraud Rules Seed Data (All 30 Rules from HLD)

```sql
-- V9__fraud_rules_seed_data.sql
-- Seeds all 30 fraud rules with default thresholds for the seed tenant
-- Tenant: 00000000-0000-0000-0000-000000000001

-- LOCATION RULES (HLD Section 5.1)
INSERT INTO fraud_rules (tenant_id, rule_id, scenario_name, scenario_name_ar, category, detection_logic, default_action, block_type, status, parameters, priority) VALUES
('00000000-0000-0000-0000-000000000001', 'LOC_001', 'Unusual Location Change',
 'تغيير موقع غير معتاد', 'LOCATION',
 'User logs in from a new device or unfamiliar location not previously seen. New location + New device + Distance > 200 km + Within 3 hours.',
 'BLOCK', 'TEMPORARY', 'ACTIVE',
 '[{"key":"distance_km","value":"200","dataType":"INT","description":"Minimum distance in km to trigger"},{"key":"time_window_hours","value":"3","dataType":"INT","description":"Time window in hours"},{"key":"block_duration_hours","value":"72","dataType":"INT","description":"Temporary block duration"}]',
 10),

('00000000-0000-0000-0000-000000000001', 'LOC_002', 'Sudden Location Change (Same Device)',
 'تغيير مفاجئ للموقع (نفس الجهاز)', 'LOCATION',
 'User logs in from a location significantly distant from their last login within a short timeframe, using the same device. Indicates potential GPS spoofing. Same device + Distance > 200 km + Within 1 hour.',
 'BLOCK', 'TEMPORARY', 'ACTIVE',
 '[{"key":"distance_km","value":"200","dataType":"INT","description":"Minimum distance in km"},{"key":"time_window_hours","value":"1","dataType":"INT","description":"Time window in hours"},{"key":"block_duration_hours","value":"72","dataType":"INT","description":"Temporary block duration"}]',
 11),

('00000000-0000-0000-0000-000000000001', 'LOC_003', 'National Address Mismatch on Application',
 'عدم تطابق العنوان الوطني عند التقديم', 'LOCATION',
 'Loan application submitted from device location exceeding configured distance from registered national address. Mandatory callback + ALERT.',
 'ALERT', NULL, 'ACTIVE',
 '[{"key":"distance_km","value":"200","dataType":"INT","description":"Minimum distance from national address"}]',
 12);

-- DEVICE RULES (HLD Section 5.2)
INSERT INTO fraud_rules (tenant_id, rule_id, scenario_name, scenario_name_ar, category, detection_logic, default_action, block_type, status, parameters, priority) VALUES
('00000000-0000-0000-0000-000000000001', 'DEV_001', 'Multiple Accounts on Same Device',
 'حسابات متعددة على نفس الجهاز', 'DEVICE',
 'Multiple different customers register new accounts using identical Device ID within 48 hours. 3 accounts + Same Device ID.',
 'BLOCK', 'TEMPORARY', 'ACTIVE',
 '[{"key":"max_accounts","value":"3","dataType":"INT","description":"Max accounts per device"},{"key":"time_window_hours","value":"48","dataType":"INT","description":"Time window"},{"key":"escalate_to_permanent_on_repeat","value":"true","dataType":"BOOLEAN","description":"Escalate to PERMANENT on second occurrence"}]',
 20),

('00000000-0000-0000-0000-000000000001', 'DEV_002', 'Multiple Loan Applications on Same Device',
 'طلبات تمويل متعددة من نفس الجهاز', 'DEVICE',
 'Multiple different customers submit loan applications from same Device ID within 48 hours.',
 'BLOCK', 'TEMPORARY', 'ACTIVE',
 '[{"key":"max_customers","value":"2","dataType":"INT","description":"Max distinct customers"},{"key":"max_applications","value":"2","dataType":"INT","description":"Max applications"},{"key":"time_window_hours","value":"48","dataType":"INT","description":"Time window"}]',
 21),

('00000000-0000-0000-0000-000000000001', 'DEV_003', 'Blacklisted Device Registration or Login',
 'تسجيل أو دخول من جهاز محظور', 'DEVICE',
 'Device previously associated with confirmed fraud attempts to register or login.',
 'BLOCK', 'PERMANENT', 'ACTIVE',
 '[]',
 22);

-- GEOGRAPHIC & ACCESS CONTROL RULES (HLD Section 5.3)
INSERT INTO fraud_rules (tenant_id, rule_id, scenario_name, scenario_name_ar, category, detection_logic, default_action, block_type, status, parameters, priority) VALUES
('00000000-0000-0000-0000-000000000001', 'GEO_001', 'Blacklisted Country Access',
 'وصول من دولة محظورة', 'GEOGRAPHIC_ACCESS',
 'Customer attempts loan application while IP/GPS resolves to blacklisted country.',
 'HOLD', 'TEMPORARY', 'ACTIVE',
 '[]',
 30),

('00000000-0000-0000-0000-000000000001', 'ACC_001', 'VPN or Proxy Usage Detected',
 'اكتشاف استخدام VPN أو بروكسي', 'GEOGRAPHIC_ACCESS',
 'User accesses platform while routing traffic through VPN or proxy.',
 'BLOCK', 'SESSION', 'ACTIVE',
 '[]',
 31),

('00000000-0000-0000-0000-000000000001', 'ACC_002', 'Jailbroken or Rooted Device',
 'جهاز مكسور الحماية', 'GEOGRAPHIC_ACCESS',
 'Device shows evidence of OS-level security tampering (jailbreak on iOS or root on Android).',
 'BLOCK', 'SESSION', 'ACTIVE',
 '[]',
 32);

-- IBAN & FINANCIAL RULES (HLD Section 5.4)
INSERT INTO fraud_rules (tenant_id, rule_id, scenario_name, scenario_name_ar, category, detection_logic, default_action, block_type, status, parameters, priority) VALUES
('00000000-0000-0000-0000-000000000001', 'FIN_001', 'Multiple Accounts Disbursing to Same IBAN',
 'حسابات متعددة تصرف لنفس الآيبان', 'FINANCIAL',
 'Loan disbursements from 2+ different customer accounts directed to the same beneficiary IBAN.',
 'HOLD', 'TEMPORARY', 'ACTIVE',
 '[{"key":"min_distinct_accounts","value":"2","dataType":"INT","description":"Minimum distinct accounts sharing IBAN"}]',
 40),

('00000000-0000-0000-0000-000000000001', 'FIN_002', 'Loan Amount vs Transfer Amount Mismatch',
 'عدم تطابق مبلغ القرض مع مبلغ التحويل', 'FINANCIAL',
 'Actual transfer amount does not match approved loan amount.',
 'HOLD', 'TEMPORARY', 'ACTIVE',
 '[]',
 41),

('00000000-0000-0000-0000-000000000001', 'FIN_003', 'Excessive Loan Applications',
 'طلبات تمويل مفرطة', 'FINANCIAL',
 'Customer submits more applications than allowed threshold within monthly or annual period.',
 'HOLD', 'TEMPORARY', 'ACTIVE',
 '[{"key":"max_per_month","value":"2","dataType":"INT","description":"Max applications per month"},{"key":"max_per_year","value":"4","dataType":"INT","description":"Max applications per year"}]',
 42);

-- PAYMENT CARD RULES (HLD Section 5.5)
INSERT INTO fraud_rules (tenant_id, rule_id, scenario_name, scenario_name_ar, category, detection_logic, default_action, block_type, status, parameters, priority) VALUES
('00000000-0000-0000-0000-000000000001', 'CARD_001', 'International Card Used for Installment',
 'بطاقة دولية مستخدمة للقسط', 'PAYMENT_CARD',
 'Customer pays installment using card issued by foreign (non-local) bank.',
 'ALERT', NULL, 'ACTIVE',
 '[{"key":"local_country","value":"SA","dataType":"STRING","description":"Local country code"}]',
 50),

('00000000-0000-0000-0000-000000000001', 'CARD_002', 'Frequent Card Changes per Account',
 'تغيير متكرر للبطاقات', 'PAYMENT_CARD',
 'Customer uses 3+ distinct cards for installment payments.',
 'ALERT', NULL, 'ACTIVE',
 '[{"key":"max_distinct_cards","value":"3","dataType":"INT","description":"Max distinct cards before alert"}]',
 51),

('00000000-0000-0000-0000-000000000001', 'CARD_003', 'Cardholder Name Mismatch',
 'عدم تطابق اسم حامل البطاقة', 'PAYMENT_CARD',
 'Card name does not match registered customer name.',
 'ALERT', NULL, 'ACTIVE',
 '[{"key":"use_fuzzy_match","value":"true","dataType":"BOOLEAN","description":"Use fuzzy name matching"}]',
 52);

-- TRANSACTION MONITORING RULES (HLD Section 5.6) — TMO-001 to TMO-017
INSERT INTO fraud_rules (tenant_id, rule_id, scenario_name, scenario_name_ar, category, detection_logic, default_action, block_type, status, parameters, priority) VALUES
('00000000-0000-0000-0000-000000000001', 'TMO_001', 'High-Frequency Loan Applications (Velocity)',
 'طلبات تمويل عالية التكرار', 'TRANSACTION_MONITORING',
 'Same customer submits >3 applications within 24-hour rolling window.',
 'HOLD', 'TEMPORARY', 'ACTIVE',
 '[{"key":"max_applications","value":"3","dataType":"INT","description":"Max applications in window"},{"key":"rolling_window_hours","value":"24","dataType":"INT","description":"Rolling window"}]',
 60),

('00000000-0000-0000-0000-000000000001', 'TMO_002', 'Application Rate Limit Breach (Monthly/Annual)',
 'تجاوز حد الطلبات الشهري/السنوي', 'TRANSACTION_MONITORING',
 'Customer exceeded max applications per month or year.',
 'BLOCK', 'APPLICATION_LEVEL', 'ACTIVE',
 '[{"key":"max_per_month","value":"2","dataType":"INT","description":"Max per month"},{"key":"max_per_year","value":"4","dataType":"INT","description":"Max per year"}]',
 61),

('00000000-0000-0000-0000-000000000001', 'TMO_003', 'Duplicate Loan Application Submission',
 'تقديم طلب تمويل مكرر', 'TRANSACTION_MONITORING',
 'Same customer + same product + same amount within 1 hour.',
 'BLOCK', 'APPLICATION_LEVEL', 'ACTIVE',
 '[{"key":"time_window_hours","value":"1","dataType":"INT","description":"Dedup window"}]',
 62),

('00000000-0000-0000-0000-000000000001', 'TMO_004', 'Late IBAN Substitution Before Disbursement',
 'تغيير الآيبان المتأخر قبل الصرف', 'TRANSACTION_MONITORING',
 'IBAN changed within 24 hours of scheduled disbursement.',
 'HOLD', 'DISBURSEMENT_LEVEL', 'ACTIVE',
 '[{"key":"min_hours_before_disbursement","value":"24","dataType":"INT","description":"Min hours before disbursement"}]',
 63),

('00000000-0000-0000-0000-000000000001', 'TMO_005', 'Multiple Disbursements to Same Beneficiary IBAN (Cross-Account)',
 'صرف متعدد لنفس الآيبان (عبر حسابات)', 'TRANSACTION_MONITORING',
 '2+ customer accounts have same beneficiary IBAN.',
 'HOLD', 'TEMPORARY', 'ACTIVE',
 '[{"key":"min_distinct_accounts","value":"2","dataType":"INT","description":"Min accounts sharing IBAN"}]',
 64),

('00000000-0000-0000-0000-000000000001', 'TMO_006', 'Disbursement Amount Mismatch',
 'عدم تطابق مبلغ الصرف', 'TRANSACTION_MONITORING',
 'Disbursed amount differs from approved loan amount.',
 'HOLD', 'PERMANENT', 'ACTIVE',
 '[]',
 65),

('00000000-0000-0000-0000-000000000001', 'TMO_007', 'Disbursement to Unverified or Mismatched IBAN',
 'صرف لآيبان غير موثق', 'TRANSACTION_MONITORING',
 'Disbursement to IBAN that is UNVERIFIED or name mismatch.',
 'BLOCK', 'DISBURSEMENT_LEVEL', 'ACTIVE',
 '[]',
 66),

('00000000-0000-0000-0000-000000000001', 'TMO_008', 'Same-Day or Near-Immediate Full Repayment',
 'سداد كامل فوري (مؤشر غسيل أموال)', 'TRANSACTION_MONITORING',
 'Full repayment within 48 hours of disbursement — money laundering indicator.',
 'ALERT', NULL, 'ACTIVE',
 '[{"key":"repayment_window_hours","value":"48","dataType":"INT","description":"Hours after disbursement"}]',
 67),

('00000000-0000-0000-0000-000000000001', 'TMO_009', 'Installment Payment from Third-Party Source',
 'دفع قسط من مصدر طرف ثالث', 'TRANSACTION_MONITORING',
 'Payment from IBAN or card not registered to borrower.',
 'HOLD', 'PAYMENT_LEVEL', 'ACTIVE',
 '[]',
 68),

('00000000-0000-0000-0000-000000000001', 'TMO_010', 'Repeated Payment Reversal or Refund Pattern',
 'نمط عكس/استرداد دفعات متكرر', 'TRANSACTION_MONITORING',
 '2+ reversals in single loan lifecycle or 3+ across multiple loans.',
 'HOLD', 'TEMPORARY', 'ACTIVE',
 '[{"key":"max_reversals_per_loan","value":"2","dataType":"INT","description":"Max reversals per loan"},{"key":"max_reversals_total","value":"3","dataType":"INT","description":"Max reversals across all loans"}]',
 69),

('00000000-0000-0000-0000-000000000001', 'TMO_011', 'Installment Payment Amount Discrepancy',
 'عدم تطابق مبلغ القسط', 'TRANSACTION_MONITORING',
 'Payment amount does not match expected installment.',
 'HOLD', 'PAYMENT_LEVEL', 'ACTIVE',
 '[{"key":"tolerance_percent","value":"5","dataType":"DECIMAL","description":"Tolerance percentage (+/-)"}]',
 70),

('00000000-0000-0000-0000-000000000001', 'TMO_012', 'Dormant Account Sudden Transaction Activity',
 'نشاط مفاجئ لحساب خامل', 'TRANSACTION_MONITORING',
 'Account inactive >90 days suddenly submits loan application or login.',
 'HOLD', 'TEMPORARY', 'ACTIVE',
 '[{"key":"dormant_days","value":"90","dataType":"INT","description":"Days of inactivity to consider dormant"}]',
 71),

('00000000-0000-0000-0000-000000000001', 'TMO_013', 'Unusual Transaction Time-of-Day',
 'وقت معاملة غير معتاد', 'TRANSACTION_MONITORING',
 'Transaction submitted between 01:00-04:00 local time — first occurrence for this account.',
 'ALERT', NULL, 'ACTIVE',
 '[{"key":"unusual_hour_start","value":"1","dataType":"INT","description":"Start hour (24h)"},{"key":"unusual_hour_end","value":"4","dataType":"INT","description":"End hour (24h)"}]',
 72),

('00000000-0000-0000-0000-000000000001', 'TMO_014', 'Round-Number Transaction Pattern',
 'نمط معاملات بأرقام مدورة', 'TRANSACTION_MONITORING',
 '3+ consecutive round-number transactions from same account.',
 'ALERT', NULL, 'ACTIVE',
 '[{"key":"round_denomination","value":"500","dataType":"INT","description":"Round denomination"},{"key":"min_consecutive","value":"3","dataType":"INT","description":"Min consecutive transactions"}]',
 73),

('00000000-0000-0000-0000-000000000001', 'TMO_015', 'High-Volume Proof of Indebtedness Requests',
 'طلبات شهادات مديونية مفرطة', 'TRANSACTION_MONITORING',
 '>3 proof-of-indebtedness requests within 7 days.',
 'HOLD', 'TEMPORARY', 'ACTIVE',
 '[{"key":"max_requests","value":"3","dataType":"INT","description":"Max requests"},{"key":"time_window_days","value":"7","dataType":"INT","description":"Time window in days"}]',
 74),

('00000000-0000-0000-0000-000000000001', 'TMO_016', 'Early Repayment Followed by Immediate Re-Application',
 'سداد مبكر متبوع بطلب فوري', 'TRANSACTION_MONITORING',
 'Early full repayment + new loan application within 24 hours.',
 'HOLD', 'TEMPORARY', 'ACTIVE',
 '[{"key":"reapplication_window_hours","value":"24","dataType":"INT","description":"Hours after early repayment"}]',
 75),

('00000000-0000-0000-0000-000000000001', 'TMO_017', 'Coordinated Application Spike (Tenant-Level)',
 'ارتفاع منسق في الطلبات (مستوى المستأجر)', 'TRANSACTION_MONITORING',
 'Application volume >3x 30-day rolling daily average within 2-hour window.',
 'ALERT', NULL, 'ACTIVE',
 '[{"key":"spike_multiplier","value":"3.0","dataType":"DECIMAL","description":"Multiplier over daily average"},{"key":"rolling_window_hours","value":"2","dataType":"INT","description":"Spike detection window"},{"key":"baseline_days","value":"30","dataType":"INT","description":"Days for baseline average"}]',
 76);
```

### V10 — EDD Schema (AML Excel — EDD Sheet)

```sql
-- V10__edd_schema.sql

CREATE TYPE edd_status AS ENUM ('REQUIRED', 'SUBMITTED', 'APPROVED', 'REJECTED');

CREATE TABLE edd_questions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    question_code   VARCHAR(50) NOT NULL,       -- SOURCE_OF_INCOME, SOURCE_OF_WEALTH, WEALTH_VALUE
    question_en     VARCHAR(500) NOT NULL,
    question_ar     VARCHAR(500) NOT NULL,
    answer_type     VARCHAR(20) NOT NULL,        -- SINGLE_SELECT, FREE_TEXT, RANGE_SELECT
    mandatory       BOOLEAN NOT NULL DEFAULT TRUE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_edd_question UNIQUE (tenant_id, question_code)
);

CREATE TABLE edd_answer_options (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    question_id     UUID NOT NULL REFERENCES edd_questions(id),
    answer_code     VARCHAR(50) NOT NULL,
    answer_en       VARCHAR(200) NOT NULL,
    answer_ar       VARCHAR(200) NOT NULL,
    sort_order      INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_edd_answer UNIQUE (question_id, answer_code)
);

CREATE TABLE edd_requirements (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    customer_id         VARCHAR(255) NOT NULL,
    aml_assessment_id   UUID,
    triggering_risk_level VARCHAR(20) NOT NULL,
    triggering_score    INT NOT NULL,
    status              edd_status NOT NULL DEFAULT 'REQUIRED',
    required_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    submitted_at        TIMESTAMPTZ,
    reviewed_at         TIMESTAMPTZ,
    reviewed_by         UUID,
    review_note         TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version             INT NOT NULL DEFAULT 1
);

CREATE TABLE edd_submissions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    edd_requirement_id  UUID NOT NULL REFERENCES edd_requirements(id),
    question_id         UUID NOT NULL REFERENCES edd_questions(id),
    answer_code         VARCHAR(50),
    free_text_answer    TEXT,
    submitted_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_edd_req_customer ON edd_requirements(tenant_id, customer_id);
CREATE INDEX idx_edd_req_status ON edd_requirements(tenant_id, status);

-- SEED DATA (from AML Excel — EDD Sheet)
INSERT INTO edd_questions (tenant_id, question_code, question_en, question_ar, answer_type, mandatory, sort_order) VALUES
('00000000-0000-0000-0000-000000000001', 'EDD_SOURCE_OF_INCOME', 'Source of income', 'مصدر الدخل', 'SINGLE_SELECT', TRUE, 1),
('00000000-0000-0000-0000-000000000001', 'EDD_SOURCE_OF_WEALTH', 'Source of wealth', 'مصدر الثروة', 'FREE_TEXT', TRUE, 2),
('00000000-0000-0000-0000-000000000001', 'EDD_WEALTH_VALUE', 'Wealth value', 'قيمة الثروة', 'SINGLE_SELECT', TRUE, 3);

-- EDD Source of Income options (different from KYC — per Excel EDD sheet)
INSERT INTO edd_answer_options (tenant_id, question_id, answer_code, answer_en, answer_ar, sort_order)
SELECT '00000000-0000-0000-0000-000000000001', q.id, v.code, v.en, v.ar, v.ord
FROM edd_questions q
CROSS JOIN (VALUES
    ('SALARY', 'Salary', 'راتب', 1),
    ('FAMILY_SUPPORT', 'Family support', 'دعم الاسرة', 2),
    ('REAL_ESTATE', 'Real estate', 'عقارات', 3),
    ('BUSINESS', 'Business', 'اعمال', 4),
    ('ADVANCE', 'Advance', 'سلفة', 5),
    ('INVESTMENT_RETURNS', 'Investment returns', 'عائدات اسثمارية', 6)
) AS v(code, en, ar, ord)
WHERE q.question_code = 'EDD_SOURCE_OF_INCOME'
AND q.tenant_id = '00000000-0000-0000-0000-000000000001';

-- EDD Wealth Value options
INSERT INTO edd_answer_options (tenant_id, question_id, answer_code, answer_en, answer_ar, sort_order)
SELECT '00000000-0000-0000-0000-000000000001', q.id, v.code, v.en, v.ar, v.ord
FROM edd_questions q
CROSS JOIN (VALUES
    ('WEALTH_0_50K', '50,000 and below', '50,000 وأقل', 1),
    ('WEALTH_50K_100K', '50k to 100k', '50 ألف إلى 100 ألف', 2),
    ('WEALTH_100K_250K', '100k to 250k', '100 ألف إلى 250 ألف', 3),
    ('WEALTH_ABOVE_200K', 'Above 200k', 'أكثر من 200 ألف', 4)
) AS v(code, en, ar, ord)
WHERE q.question_code = 'EDD_WEALTH_VALUE'
AND q.tenant_id = '00000000-0000-0000-0000-000000000001';

ALTER TABLE edd_requirements ENABLE ROW LEVEL SECURITY;
```

### V11 — Full Occupation Seed Data (3,599 from AML Excel)

```sql
-- V11__aml_full_occupation_seed.sql
-- This migration will contain all 3,599 occupations from the Excel
-- with code, Arabic name, English name, and risk level
-- (Generated from 'Occupations list' sheet)
-- Example format:
-- INSERT INTO aml_occupation_risk_levels (tenant_id, occupation_code, name_ar, name_en, risk_level) VALUES ...
-- [Full 3,599 rows from Excel]
```

### V12 — KYC Questionnaire Options (AML Excel — KYC Sheet)

```sql
-- V12__kyc_questionnaire_options.sql
-- Bilingual KYC question options for API serving

CREATE TABLE kyc_questionnaire_options (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    question_code   VARCHAR(50) NOT NULL,
    answer_code     VARCHAR(50) NOT NULL,
    answer_en       VARCHAR(200) NOT NULL,
    answer_ar       VARCHAR(200) NOT NULL,
    sort_order      INT NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_kyc_option UNIQUE (tenant_id, question_code, answer_code)
);

-- Source of Income (KYC Sheet)
INSERT INTO kyc_questionnaire_options (tenant_id, question_code, answer_code, answer_en, answer_ar, sort_order) VALUES
('00000000-0000-0000-0000-000000000001', 'SOURCE_OF_INCOME', 'SALARY', 'Salary', 'راتب', 1),
('00000000-0000-0000-0000-000000000001', 'SOURCE_OF_INCOME', 'REAL_ESTATE', 'Real estate', 'عقارات', 2),
('00000000-0000-0000-0000-000000000001', 'SOURCE_OF_INCOME', 'INVESTMENT_RETURNS', 'Investment Returns', 'عوائد استثمارية', 3),
('00000000-0000-0000-0000-000000000001', 'SOURCE_OF_INCOME', 'RENTAL_INCOME', 'Rental Income', 'دخل ايجارات', 4),
('00000000-0000-0000-0000-000000000001', 'SOURCE_OF_INCOME', 'BUSINESS_RETURNS', 'Business returns', 'اعمال تجارية', 5);

-- Monthly Income (KYC Sheet)
INSERT INTO kyc_questionnaire_options (tenant_id, question_code, answer_code, answer_en, answer_ar, sort_order) VALUES
('00000000-0000-0000-0000-000000000001', 'MONTHLY_INCOME', 'INCOME_0_3000', '3000 less', '3000 أو أقل', 1),
('00000000-0000-0000-0000-000000000001', 'MONTHLY_INCOME', 'INCOME_3001_6000', '3001 - 6000', '3001 - 6000', 2),
('00000000-0000-0000-0000-000000000001', 'MONTHLY_INCOME', 'INCOME_6001_10000', '6001 - 10000', '6001 - 10000', 3),
('00000000-0000-0000-0000-000000000001', 'MONTHLY_INCOME', 'INCOME_10001_20000', '10001 - 20000', '10001 - 20000', 4),
('00000000-0000-0000-0000-000000000001', 'MONTHLY_INCOME', 'INCOME_20001_30000', '20001 - 30000', '20001 - 30000', 5),
('00000000-0000-0000-0000-000000000001', 'MONTHLY_INCOME', 'INCOME_30001_40000', '30001 - 40000', '30001 - 40000', 6),
('00000000-0000-0000-0000-000000000001', 'MONTHLY_INCOME', 'INCOME_40001_60000', '40001 - 60000', '40001 - 60000', 7),
('00000000-0000-0000-0000-000000000001', 'MONTHLY_INCOME', 'INCOME_60001_80000', '60001 - 80000', '60001 - 80000', 8),
('00000000-0000-0000-0000-000000000001', 'MONTHLY_INCOME', 'INCOME_80000_ABOVE', '80000 - above', '80000 وأكثر', 9);

-- Company Name & Profession are free-text with validation (not options)
```

---

## 5. NEW API ENDPOINTS

### All APIs stay under `/api/v1/risk/` — no duplication with existing.

### 5.1 Fraud Evaluation (Core — HLD Section 3.3)

| Method | Path | Auth | Casbin | Purpose |
|--------|------|------|--------|---------|
| POST | `/api/v1/risk/fraud/evaluate` | Public (X-Tenant-Id) | — | Synchronous fraud evaluation from LOS/LMS |

### 5.2 Fraud Rule Management (Admin — HLD Section 5)

| Method | Path | Auth | Casbin | Purpose |
|--------|------|------|--------|---------|
| GET | `/api/v1/risk/fraud/rules` | JWT | risk.fraud-rules / read | List all rules for tenant |
| GET | `/api/v1/risk/fraud/rules/{ruleId}` | JWT | risk.fraud-rules / read | Get rule details + thresholds |
| PUT | `/api/v1/risk/fraud/rules/{ruleId}` | JWT | risk.fraud-rules / update | Update rule thresholds/status |
| POST | `/api/v1/risk/fraud/rules/{ruleId}/activate` | JWT | risk.fraud-rules / manage | Activate rule |
| POST | `/api/v1/risk/fraud/rules/{ruleId}/disable` | JWT | risk.fraud-rules / manage | Disable rule |
| POST | `/api/v1/risk/fraud/rules/{ruleId}/test` | JWT | risk.fraud-rules / manage | Set rule to TESTING mode |

### 5.3 Fraud Alert Management (Operations — HLD Section 3.3 Step 5-8)

| Method | Path | Auth | Casbin | Purpose |
|--------|------|------|--------|---------|
| GET | `/api/v1/risk/fraud/alerts` | JWT | risk.fraud-alerts / read | List alerts (filterable: status, priority, date) |
| GET | `/api/v1/risk/fraud/alerts/{id}` | JWT | risk.fraud-alerts / read | Get alert details |
| POST | `/api/v1/risk/fraud/alerts/{id}/assign` | JWT | risk.fraud-alerts / manage | Assign to analyst |
| POST | `/api/v1/risk/fraud/alerts/{id}/resolve` | JWT | risk.fraud-alerts / manage | Resolve alert |
| POST | `/api/v1/risk/fraud/alerts/{id}/escalate` | JWT | risk.fraud-alerts / manage | Escalate alert |
| POST | `/api/v1/risk/fraud/alerts/{id}/dismiss` | JWT | risk.fraud-alerts / manage | Dismiss as false positive |

### 5.4 Fraud Case Management (Operations — HLD Section 3.2)

| Method | Path | Auth | Casbin | Purpose |
|--------|------|------|--------|---------|
| GET | `/api/v1/risk/fraud/cases` | JWT | risk.fraud-cases / read | List cases |
| GET | `/api/v1/risk/fraud/cases/{id}` | JWT | risk.fraud-cases / read | Get case details + actions |
| POST | `/api/v1/risk/fraud/cases` | JWT | risk.fraud-cases / create | Create case (link alerts) |
| PUT | `/api/v1/risk/fraud/cases/{id}` | JWT | risk.fraud-cases / update | Update case |
| POST | `/api/v1/risk/fraud/cases/{id}/actions` | JWT | risk.fraud-cases / manage | Add action (block, contact, escalate) |
| POST | `/api/v1/risk/fraud/cases/{id}/close` | JWT | risk.fraud-cases / manage | Close case |

### 5.5 EDD Management (Compliance — AML Excel EDD Sheet)

| Method | Path | Auth | Casbin | Purpose |
|--------|------|------|--------|---------|
| GET | `/api/v1/risk/edd/questions` | Public (X-Tenant-Id) | — | Get EDD questions with options (bilingual) |
| GET | `/api/v1/risk/edd/requirements/{customerId}` | JWT | risk.edd / read | Get EDD requirement for customer |
| POST | `/api/v1/risk/edd/requirements/{customerId}/submit` | Public (X-Tenant-Id) | — | Submit EDD answers |
| POST | `/api/v1/risk/edd/requirements/{id}/review` | JWT | risk.edd / manage | Approve/reject EDD submission |
| GET | `/api/v1/risk/edd/pending` | JWT | risk.edd / read | List pending EDD reviews |

### 5.6 Extended Blacklists (Admin — HLD Sections 6, 8)

| Method | Path | Auth | Casbin | Purpose |
|--------|------|------|--------|---------|
| POST | `/api/v1/risk/blacklist/devices` | JWT | risk.blacklist / create | Blacklist device |
| DELETE | `/api/v1/risk/blacklist/devices/{deviceId}` | JWT | risk.blacklist / delete | Remove device |
| GET | `/api/v1/risk/blacklist/devices` | JWT | risk.blacklist / read | List blacklisted devices |
| POST | `/api/v1/risk/blacklist/countries` | JWT | risk.blacklist / create | Blacklist country |
| DELETE | `/api/v1/risk/blacklist/countries/{countryCode}` | JWT | risk.blacklist / delete | Remove country |
| GET | `/api/v1/risk/blacklist/countries` | JWT | risk.blacklist / read | List blacklisted countries |
| POST | `/api/v1/risk/blacklist/ibans` | JWT | risk.blacklist / create | Blacklist IBAN |
| DELETE | `/api/v1/risk/blacklist/ibans/{id}` | JWT | risk.blacklist / delete | Remove IBAN |
| GET | `/api/v1/risk/blacklist/ibans` | JWT | risk.blacklist / read | List blacklisted IBANs |

### 5.7 AML Reference Data (Admin — AML Excel)

| Method | Path | Auth | Casbin | Purpose |
|--------|------|------|--------|---------|
| GET | `/api/v1/risk/aml/reference-data/categories` | JWT | risk.aml-config / read | List AML categories + weights |
| PUT | `/api/v1/risk/aml/reference-data/categories/{id}` | JWT | risk.aml-config / update | Update category weight |
| GET | `/api/v1/risk/aml/reference-data/thresholds` | JWT | risk.aml-config / read | Get risk thresholds |
| PUT | `/api/v1/risk/aml/reference-data/thresholds` | JWT | risk.aml-config / update | Update thresholds |
| GET | `/api/v1/risk/aml/reference-data/fatf-countries` | JWT | risk.aml-config / read | List FATF countries |
| POST | `/api/v1/risk/aml/reference-data/fatf-countries` | JWT | risk.aml-config / create | Add FATF country |
| DELETE | `/api/v1/risk/aml/reference-data/fatf-countries/{code}` | JWT | risk.aml-config / delete | Remove FATF country |
| GET | `/api/v1/risk/aml/reference-data/occupations` | JWT | risk.aml-config / read | List occupations (paginated) |
| GET | `/api/v1/risk/aml/reference-data/kyc-options` | Public (X-Tenant-Id) | — | Serve KYC questionnaire options (bilingual) |

### 5.8 AML Assessment History (Compliance Reporting)

| Method | Path | Auth | Casbin | Purpose |
|--------|------|------|--------|---------|
| GET | `/api/v1/risk/aml/assessments` | JWT | risk.aml-assessments / read | List assessments (paginated, filterable) |
| GET | `/api/v1/risk/aml/assessments/{id}` | JWT | risk.aml-assessments / read | Get assessment details + breakdown |
| GET | `/api/v1/risk/aml/assessments/customer/{customerId}` | JWT | risk.aml-assessments / read | Assessment history for customer |
| POST | `/api/v1/risk/aml/assessments/reassess/{customerId}` | JWT | risk.aml-assessments / manage | Trigger manual re-assessment |

### 5.9 Risk Dashboard (Analytics)

| Method | Path | Auth | Casbin | Purpose |
|--------|------|------|--------|---------|
| GET | `/api/v1/risk/dashboard/summary` | JWT | risk.dashboard / read | Overall risk stats |
| GET | `/api/v1/risk/dashboard/aml-distribution` | JWT | risk.dashboard / read | AML risk level distribution |
| GET | `/api/v1/risk/dashboard/fraud-alerts` | JWT | risk.dashboard / read | Alert stats (open, resolved, SLA) |
| GET | `/api/v1/risk/dashboard/top-rules` | JWT | risk.dashboard / read | Most triggered rules |
| GET | `/api/v1/risk/dashboard/trend` | JWT | risk.dashboard / read | Risk trend over time |

---

## 6. NEW USE CASES & SERVICES

### 6.1 EvaluateFraudEventService (CORE — HLD Section 3.3 Flow)

```
Input: FraudEventRequestDto (from LOS/LMS)
Flow:
  1. Validate event + check idempotency (event_id)
  2. Persist raw event to fraud_events
  3. ENRICH:
     a. IP geolocation → country, city (IpGeolocationEnricher)
     b. VPN/proxy detection (VpnProxyDetector)
     c. Distance from last known location (DistanceCalculationEnricher)
     d. Device integrity check (DeviceIntegrityEnricher)
     e. Update fraud_user_profiles with latest data
  4. Load active fraud_rules for tenant
  5. EVALUATE: FraudRuleEngine runs all active rules against enriched event
     - Each rule returns: triggered (bool), decision, detail, scoreContribution
     - Only rules applicable to event_type are evaluated
  6. DECIDE: FraudDecisionEngine consolidates rule outputs
     - Highest-severity decision wins (BLOCK > HOLD > ALERT > ALLOW)
     - Compute composite risk score (0-100)
  7. Persist fraud_evaluations with triggered_rules JSONB
  8. If ALERT or BLOCK → create fraud_alert (SLA deadline = now + 60s)
  9. Publish Kafka event: financing.risk.fraud.evaluated
  10. Return FraudEvaluationResponseDto synchronously
Output: FraudEvaluationResponseDto (decision, score, triggered rules, < 500ms)
```

### 6.2 Fraud Rule Engine (Domain Service — Pure Logic)

```
FraudRuleEngine.evaluate(FraudEvent enrichedEvent, List<FraudRule> activeRules)
  → List<RuleEvaluationResult>

- Each rule class implements: FraudRuleEvaluator interface
- interface FraudRuleEvaluator {
      FraudRuleId getRuleId();
      Set<FraudEventType> applicableEventTypes();
      RuleEvaluationResult evaluate(FraudEvent event, List<RuleParameter> params);
  }
- Rules are registered as Spring beans via FraudRuleEngineConfig
- Engine filters rules by event type, then evaluates in priority order
```

### 6.3 Fraud Decision Engine (Domain Service — Pure Logic)

```
FraudDecisionEngine.decide(List<RuleEvaluationResult> ruleResults)
  → FraudCompositeScore

- Priority: BLOCK > HOLD > ALERT > ALLOW
- If any rule returns BLOCK → final decision = BLOCK
- If any rule returns HOLD → final decision = HOLD
- If any rule returns ALERT → final decision = ALERT
- Otherwise → ALLOW
- Composite score = sum of all triggered rule scoreContributions (capped at 100)
- RiskLevel mapping: ≤30 LOW, ≤60 MEDIUM, ≤80 HIGH, >80 CRITICAL
```

---

## 7. NEW INFRASTRUCTURE COMPONENTS

### 7.1 Fraud Rule Evaluator Interface

```java
// Each of the 30 rules implements this interface
public interface FraudRuleEvaluator {
    FraudRuleId getRuleId();
    Set<FraudEventType> applicableEventTypes();
    RuleEvaluationResult evaluate(FraudEvent event, List<RuleParameter> parameters,
                                  FraudRuleContext context);
}

// Context provides access to historical data needed by rules
public record FraudRuleContext(
    FraudUserProfile userProfile,          // last login, registered address, IBAN, etc.
    List<SessionEvent> recentSessions,     // last N sessions for this customer
    List<FraudEvent> recentEvents,         // last N events for this customer
    List<String> blacklistedDevices,       // for DEV-003
    List<String> blacklistedCountries,     // for GEO-001
    List<String> blacklistedIbans,         // for FIN-001
    long applicationCountLast24h,          // for TMO-001
    long applicationCountThisMonth,        // for TMO-002
    long applicationCountThisYear,         // for TMO-002
    int distinctCardsUsed,                 // for CARD-002
    int paymentReversalsThisLoan,          // for TMO-010
    long tenantDailyAppAverage30d          // for TMO-017
) {}
```

### 7.2 Rule Applicability Map

```
ONBOARDING     → LOC-001, DEV-001, DEV-003, GEO-001, ACC-001, ACC-002
LOGIN          → LOC-001, LOC-002, DEV-003, ACC-001, ACC-002, TMO-012, TMO-013
LOAN_APPLICATION → LOC-003, DEV-002, GEO-001, FIN-003, TMO-001, TMO-002, TMO-003, TMO-013, TMO-014, TMO-016, TMO-017
DISBURSEMENT   → FIN-001, FIN-002, TMO-004, TMO-005, TMO-006, TMO-007
REPAYMENT      → CARD-001, CARD-002, CARD-003, TMO-008, TMO-009, TMO-010, TMO-011
EARLY_REPAYMENT → TMO-008, TMO-016
IBAN_UPDATE    → TMO-004
PROOF_OF_INDEBTEDNESS → TMO-015
ACCOUNT_UPDATE → TMO-012
```

### 7.3 Kafka Topics

| Topic | Publisher | Consumer | Purpose |
|-------|-----------|----------|---------|
| `financing.loan.application.submitted` | LOS | Risk Service | Trigger fraud evaluation for loan apps |
| `financing.loan.disbursed` | LMS | Risk Service | Trigger disbursement rules |
| `financing.loan.repayment.received` | LMS | Risk Service | Trigger repayment rules |
| `financing.customer.iban.updated` | Customer Service | Risk Service | Trigger IBAN change rules |
| `financing.customer.onboarded` | Customer Service | Risk Service | Register user in fraud profiles |
| `financing.risk.fraud.evaluated` | Risk Service | Notification Service | Fraud alerts for operations |
| `financing.risk.fraud.alert.created` | Risk Service | Admin Dashboard | New alert notification |
| `financing.risk.aml.scored` | Risk Service | Customer Service | AML score result |

### 7.4 Enrichment Pipeline (HLD Section 3.3 Step 2)

```
FraudEvent (raw)
  → IpGeolocationEnricher      → adds: ipCountry, ipCity, coordinates
  → VpnProxyDetector           → adds: vpnDetected, proxyDetected
  → DeviceIntegrityEnricher    → adds: integrityStatus (CLEAN/JAILBROKEN/ROOTED)
  → DistanceCalculationEnricher → adds: distanceFromLastKm, timeSinceLastLogin
  → FraudEvent (enriched)

Note: Initially these will be STUB implementations (similar to KYC stubs).
      In production, plug in real IP intelligence API (MaxMind, IP2Location, etc.)
```

---

## 8. RULE ENGINE ARCHITECTURE

```
                    ┌──────────────────────┐
                    │   LOS / LMS / Other  │
                    │   (Event Publisher)   │
                    └──────────┬───────────┘
                               │
                    ┌──────────▼───────────┐
                    │  POST /fraud/evaluate │  ← Sync API (< 500ms)
                    │  OR Kafka Consumer    │  ← Async for non-blocking events
                    └──────────┬───────────┘
                               │
                    ┌──────────▼───────────┐
                    │  Event Persistence   │  → fraud_events table
                    └──────────┬───────────┘
                               │
                    ┌──────────▼───────────┐
                    │  Enrichment Pipeline │
                    │  ├── IP Geolocation  │
                    │  ├── VPN/Proxy Check │
                    │  ├── Device Integrity│
                    │  └── Distance Calc   │
                    └──────────┬───────────┘
                               │
                    ┌──────────▼───────────┐
                    │    Rule Engine        │
                    │                      │
                    │  For each active rule │
                    │  matching event type: │
                    │  ├── Load parameters │
                    │  ├── Load context    │
                    │  ├── Evaluate rule   │
                    │  └── Collect result  │
                    └──────────┬───────────┘
                               │
                    ┌──────────▼───────────┐
                    │   Decision Engine    │
                    │                      │
                    │  BLOCK > HOLD > ALERT│
                    │  Composite Score 0-100│
                    └──────────┬───────────┘
                               │
              ┌────────────────┼────────────────┐
              │                │                │
    ┌─────────▼──────┐ ┌──────▼───────┐ ┌──────▼────────┐
    │  Persist Result │ │ Create Alert │ │ Return to     │
    │  fraud_evaluations│ │ fraud_alerts│ │ Caller (sync) │
    └────────────────┘ └──────┬───────┘ └───────────────┘
                              │
                    ┌─────────▼────────┐
                    │  Kafka Publish   │
                    │  + Notification  │
                    └──────────────────┘
```

---

## 9. COMPLETE API ENDPOINT COUNT

### Existing (KEEP — No Changes)

| Controller | Endpoints |
|------------|-----------|
| InternalChecksController | 1 |
| AmlRiskScoreController | 1 |
| BlacklistController | 8 |
| CreditScoringController | 4 |
| **Subtotal** | **14** |

### New (TO BUILD)

| Controller | Endpoints |
|------------|-----------|
| FraudEvaluationController | 1 |
| FraudRuleController | 6 |
| FraudAlertController | 6 |
| FraudCaseController | 6 |
| EddController | 5 |
| DeviceBlacklistController | 3 |
| CountryBlacklistController | 3 |
| IbanBlacklistController | 3 |
| AmlReferenceDataController | 9 |
| AmlAssessmentHistoryController | 4 |
| RiskDashboardController | 5 |
| **Subtotal** | **51** |

### **TOTAL: 65 endpoints** (14 existing + 51 new)

---

## 10. SPRINT BREAKDOWN

### Sprint 1: Fraud Foundation (Core Engine)
- V8 migration (fraud tables)
- V9 migration (30 rules seed)
- FraudEvent, FraudRule, FraudDecision domain models
- FraudRuleEvaluator interface
- FraudRuleEngine (domain service)
- FraudDecisionEngine (domain service)
- EvaluateFraudEventService (use case)
- FraudEvaluationController (POST /fraud/evaluate)
- FraudEventRepository, FraudRuleRepository
- Stub enrichment pipeline

### Sprint 2: Location + Device + Access Rules (9 rules)
- LOC-001, LOC-002, LOC-003
- DEV-001, DEV-002, DEV-003
- GEO-001, ACC-001, ACC-002
- GeoDistanceCalculator (Haversine)
- DeviceInfo, LocationData, SessionEvent models
- Session event tracking
- Device/country blacklist tables + repos

### Sprint 3: Financial + Card + TMO Velocity Rules (9 rules)
- FIN-001, FIN-002, FIN-003
- CARD-001, CARD-002, CARD-003
- TMO-001, TMO-002, TMO-003
- TransactionEvent, PaymentSource models
- fraud_user_profiles table + repo

### Sprint 4: TMO Disbursement + Repayment Rules (8 rules)
- TMO-004, TMO-005, TMO-006, TMO-007
- TMO-008, TMO-009, TMO-010, TMO-011

### Sprint 5: TMO Behavioral Rules (6 rules)
- TMO-012, TMO-013, TMO-014
- TMO-015, TMO-016, TMO-017

### Sprint 6: Fraud Operations (Alerts + Cases)
- FraudAlert, FraudCase domain models
- FraudAlertController (6 endpoints)
- FraudCaseController (6 endpoints)
- ManageFraudAlertsService
- ManageFraudCasesService
- Kafka alert publishing

### Sprint 7: EDD + AML Enhancements
- V10 migration (EDD schema + seed)
- V11 migration (full occupations)
- V12 migration (KYC options)
- EddController (5 endpoints)
- EDD trigger logic (AML score ≥ HIGH)
- AmlReferenceDataController (9 endpoints)
- AmlAssessmentHistoryController (4 endpoints)

### Sprint 8: Admin + Dashboard + Extended Blacklists
- FraudRuleController (6 endpoints)
- DeviceBlacklistController (3 endpoints)
- CountryBlacklistController (3 endpoints)
- IbanBlacklistController (3 endpoints)
- RiskDashboardController (5 endpoints)

### Sprint 9: Kafka Integration + Notifications
- Kafka consumers for LOS/LMS events
- Kafka producers for fraud alerts
- FraudNotificationAdapter
- Async event processing

### Sprint 10: Production Readiness
- Enable Redis velocity checks
- Performance testing (< 500ms)
- 5-year data retention policy
- Full audit trail verification
- Degraded-mode fallback
- Security hardening review

---

## FILE COUNT SUMMARY

| Category | Existing | New | Total |
|----------|----------|-----|-------|
| Domain Models | 25 | 32 | 57 |
| Input Ports (Use Cases) | 5 | 13 | 18 |
| Output Ports (Repos/Checks) | 14 | 13 | 27 |
| Domain Services | 1 | 4 | 5 |
| Use Case Implementations | 5 | 13 | 18 |
| DTOs | 3 | 19 | 22 |
| Infrastructure Checks | 10 | 0 | 10 |
| Infrastructure Rules | 0 | 30 | 30 |
| Infrastructure Enrichment | 0 | 4 | 4 |
| Infrastructure Repos | 4 | 12 | 16 |
| Infrastructure Messaging | 0 | 3 | 3 |
| Infrastructure Config | 3 | 2 | 5 |
| Controllers | 4 | 11 | 15 |
| Request DTOs | 3 | 3 | 6 |
| Mappers | 0 | 2 | 2 |
| DB Migrations | 7 | 5 | 12 |
| **TOTAL FILES** | **84** | **166** | **250** |

---

## OUTSOURCE-READY ARCHITECTURE

To make this sellable as a standalone service:

1. **Multi-tenant by design** — every table has `tenant_id`, every rule is tenant-configurable
2. **Pluggable enrichment** — IP geolocation, VPN detection are interface-based (swap providers)
3. **Configurable rules** — all 30 rules have configurable thresholds via admin API (not hardcoded)
4. **Event-driven ingestion** — REST API + Kafka consumers (works with any LOS/LMS)
5. **Bilingual** — all reference data (KYC, EDD, rules, alerts) in English + Arabic
6. **Self-contained DB** — no shared schema, owns all its data
7. **Standard APIs** — RESTful, OpenAPI documented, Casbin-secured
8. **Audit trail** — every evaluation, alert, case action is logged for compliance
