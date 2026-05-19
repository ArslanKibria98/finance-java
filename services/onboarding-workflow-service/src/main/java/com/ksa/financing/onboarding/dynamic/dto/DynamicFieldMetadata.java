package com.ksa.financing.onboarding.dynamic.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DynamicFieldMetadata {
    private String key;
    private String label;
    private String type;
    private boolean required;
    private String validationRegex;
    private boolean isPii;
    private int order;
}
