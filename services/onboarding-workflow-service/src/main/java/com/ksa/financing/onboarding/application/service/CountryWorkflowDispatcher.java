package com.ksa.financing.onboarding.application.service;

import com.ksa.financing.onboarding.infrastructure.persistence.repository.WorkflowConfigRepository;
import org.springframework.stereotype.Service;

@Service
public class CountryWorkflowDispatcher {

    private final WorkflowConfigRepository workflowConfigRepository;

    public CountryWorkflowDispatcher(WorkflowConfigRepository workflowConfigRepository) {
        this.workflowConfigRepository = workflowConfigRepository;
    }

    /**
     * Checks if a country should use the new dynamic onboarding engine.
     * KSA is always static (legacy). Others are checked in the dynamic config.
     */
    public boolean isDynamicWorkflow(String countryCode) {
        if ("SA".equalsIgnoreCase(countryCode)) {
            return false;
        }
        return workflowConfigRepository.existsById(countryCode.toUpperCase());
    }
}
