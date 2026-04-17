package com.ksa.financing.risk.infrastructure.config;

import com.ksa.financing.risk.domain.service.ApprovalWorkflowEvaluationEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApprovalWorkflowConfig {

    @Bean
    public ApprovalWorkflowEvaluationEngine approvalWorkflowEvaluationEngine() {
        return new ApprovalWorkflowEvaluationEngine();
    }
}
