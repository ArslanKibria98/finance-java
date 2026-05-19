package com.ksa.financing.onboarding.dynamic.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class DynamicStepMetadata {
    private String sessionId;
    private String stepName;
    private int order;
    private String status;
    private List<DynamicFieldMetadata> fields;
}
