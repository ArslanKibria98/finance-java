package com.ksa.financing.onboarding.dynamic.service;

import com.ksa.financing.onboarding.dynamic.dto.DynamicFieldMetadata;
import com.ksa.financing.onboarding.dynamic.dto.DynamicStepMetadata;
import com.ksa.financing.onboarding.infrastructure.persistence.entity.FieldConfigEntity;
import com.ksa.financing.onboarding.infrastructure.persistence.entity.StepConfigEntity;
import com.ksa.financing.onboarding.infrastructure.persistence.entity.StepSubmissionEntity;
import com.ksa.financing.onboarding.infrastructure.persistence.repository.FieldConfigRepository;
import com.ksa.financing.onboarding.infrastructure.persistence.repository.StepConfigRepository;
import com.ksa.financing.onboarding.infrastructure.persistence.repository.StepSubmissionRepository;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DynamicOnboardingService {

    private final StepConfigRepository stepConfigRepository;
    private final FieldConfigRepository fieldConfigRepository;
    private final StepSubmissionRepository stepSubmissionRepository;
    private final GenericApiClient genericApiClient;
    private final PiiVaultClient piiVaultClient;
    private final CustomerServiceClient customerServiceClient;
    private final ObjectMapper objectMapper;

    public DynamicOnboardingService(StepConfigRepository stepConfigRepository,
                                    FieldConfigRepository fieldConfigRepository,
                                    StepSubmissionRepository stepSubmissionRepository,
                                    GenericApiClient genericApiClient,
                                    PiiVaultClient piiVaultClient,
                                    CustomerServiceClient customerServiceClient,
                                    ObjectMapper objectMapper) {
        this.stepConfigRepository = stepConfigRepository;
        this.fieldConfigRepository = fieldConfigRepository;
        this.stepSubmissionRepository = stepSubmissionRepository;
        this.genericApiClient = genericApiClient;
        this.piiVaultClient = piiVaultClient;
        this.customerServiceClient = customerServiceClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Initializes a dynamic onboarding session and returns the first step metadata.
     */
    public DynamicStepMetadata initiate(String nationalId, String mobileNumber, String countryCode) {
        String sessionId = "dynamic-" + countryCode + "-" + nationalId;
        List<StepConfigEntity> steps = stepConfigRepository.findByCountryCodeOrderByOrderIndexAsc(countryCode);
        if (steps.isEmpty()) {
            throw new RuntimeException("No workflow configuration found for country: " + countryCode);
        }
        return getStepMetadata(steps.get(0), sessionId);
    }

    /**
     * Fetches the full workflow metadata for a given country.
     */
    public List<DynamicStepMetadata> getWorkflowMetadata(String countryCode) {
        List<StepConfigEntity> steps = stepConfigRepository.findByCountryCodeOrderByOrderIndexAsc(countryCode);

        return steps.stream().map(step -> {
            List<FieldConfigEntity> fields = fieldConfigRepository.findByStepIdOrderByOrderIndexAsc(step.getId());
            
            List<DynamicFieldMetadata> fieldDtos = fields.stream()
                    .map(f -> DynamicFieldMetadata.builder()
                            .key(f.getFieldKey())
                            .label(f.getFieldLabel())
                            .type(f.getFieldType())
                            .required(f.getIsMandatory())
                            .validationRegex(f.getValidationRegex())
                            .isPii(f.getIsPii())
                            .order(f.getOrderIndex())
                            .build())
                    .collect(Collectors.toList());

            return DynamicStepMetadata.builder()
                    .stepName(step.getStepName())
                    .order(step.getOrderIndex())
                    .status("pending") // Default status for metadata query
                    .fields(fieldDtos)
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * Processes a dynamic step submission.
     */
    public DynamicStepMetadata submitStep(String sessionId, String countryCode, String stepName, Map<String, Object> inputData) {
        // 1. Find step config
        StepConfigEntity step = stepConfigRepository.findByCountryCodeOrderByOrderIndexAsc(countryCode).stream()
                .filter(s -> s.getStepName().equalsIgnoreCase(stepName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Step not found: " + stepName));

        // 2. Identify and handle PII fields
        List<FieldConfigEntity> fieldConfigs = fieldConfigRepository.findByStepIdOrderByOrderIndexAsc(step.getId());
        Map<String, Object> sensitiveData = new java.util.HashMap<>();
        
        for (FieldConfigEntity f : fieldConfigs) {
            if (f.getIsPii() && inputData.containsKey(f.getFieldKey())) {
                sensitiveData.put(f.getFieldKey(), inputData.get(f.getFieldKey()));
                // Replace with token (placeholder logic - usually Vault returns token)
                inputData.put(f.getFieldKey(), "[PII_TOKENIZED]"); 
            }
        }

        if (!sensitiveData.isEmpty()) {
            piiVaultClient.storePii(sessionId, countryCode, sensitiveData);
        }

        Map<String, Object> finalData = new java.util.HashMap<>(inputData);

        // 3. Execute Step-level API if configured
        if (step.getApiUrl() != null && !step.getApiUrl().isBlank()) {
            Map<String, String> headers = parseMap(step.getApiHeaders());
            Map<String, String> paramsMapping = parseMap(step.getApiParamsMapping());
            Map<String, String> bodyMapping = parseMap(step.getApiBodyMapping());

            Map<String, Object> queryParams = new java.util.HashMap<>();
            if (paramsMapping != null) {
                paramsMapping.forEach((remoteKey, fieldKey) -> {
                    if (inputData.containsKey(fieldKey)) {
                        queryParams.put(remoteKey, inputData.get(fieldKey));
                    }
                });
            }

            Map<String, Object> requestBody = new java.util.HashMap<>();
            if (bodyMapping != null) {
                bodyMapping.forEach((remoteKey, fieldKey) -> {
                    if (inputData.containsKey(fieldKey)) {
                        requestBody.put(remoteKey, inputData.get(fieldKey));
                    }
                });
            } else {
                // Default: send all data if no mapping is provided
                requestBody.putAll(inputData);
            }

            Map<String, Object> apiResponse = genericApiClient.execute(
                step.getApiUrl(), 
                step.getApiMethod(), 
                headers, 
                queryParams, 
                requestBody
            );
            finalData.put("_third_party_response", apiResponse);
        }

        // 4. Save submission (The JSON Sink)
        StepSubmissionEntity submission = new StepSubmissionEntity();
        submission.setSessionId(sessionId);
        submission.setCountryCode(countryCode);
        submission.setStepId(step.getId());
        submission.setRawData(finalData);
        stepSubmissionRepository.save(submission);

        // 4. Return metadata for next step
        int nextOrder = step.getOrderIndex() + 1;
        List<StepConfigEntity> allSteps = stepConfigRepository.findByCountryCodeOrderByOrderIndexAsc(countryCode);
        
        return allSteps.stream()
                .filter(s -> s.getOrderIndex() == nextOrder)
                .findFirst()
                .map(s -> {
                    // Return metadata for next step
                    return getStepMetadata(s, sessionId);
                })
                .orElseGet(() -> {
                    // All steps completed -> Finalize
                    finalizeOnboarding(sessionId, countryCode);
                    return DynamicStepMetadata.builder()
                            .sessionId(sessionId)
                            .stepName("COMPLETED")
                            .status("COMPLETED")
                            .fields(java.util.Collections.emptyList())
                            .build();
                });
    }

    /**
     * Aggregates all step data and creates a permanent CIF.
     */
    private String finalizeOnboarding(String sessionId, String countryCode) {
        List<StepSubmissionEntity> submissions = stepSubmissionRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        
        Map<String, Object> aggregatedData = new java.util.HashMap<>();
        for (StepSubmissionEntity sub : submissions) {
            aggregatedData.putAll(sub.getRawData());
        }

        // Apply Final Mapping (Placeholder for Admin-defined mapping)
        Map<String, Object> cifPayload = new java.util.HashMap<>();
        cifPayload.put("nationalId", aggregatedData.get("cnic_number"));
        cifPayload.put("mobileNumber", aggregatedData.get("mobileNumber"));
        cifPayload.put("countryCode", countryCode);
        cifPayload.put("globalUid", sessionId);
        
        // Add all other dynamic fields
        cifPayload.put("dynamicData", aggregatedData);

        return customerServiceClient.createCif(cifPayload);
    }

    private DynamicStepMetadata getStepMetadata(StepConfigEntity step, String sessionId) {
        List<FieldConfigEntity> fields = fieldConfigRepository.findByStepIdOrderByOrderIndexAsc(step.getId());
        List<DynamicFieldMetadata> fieldDtos = fields.stream()
                .map(f -> DynamicFieldMetadata.builder()
                        .key(f.getFieldKey())
                        .label(f.getFieldLabel())
                        .type(f.getFieldType())
                        .required(f.getIsMandatory())
                        .validationRegex(f.getValidationRegex())
                        .isPii(f.getIsPii())
                        .order(f.getOrderIndex())
                        .build())
                .collect(Collectors.toList());

        return DynamicStepMetadata.builder()
                .sessionId(sessionId)
                .stepName(step.getStepName())
                .order(step.getOrderIndex())
                .status("current")
                .fields(fieldDtos)
                .build();
    }

    private Map<String, String> parseMap(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            return null;
        }
    }
}
