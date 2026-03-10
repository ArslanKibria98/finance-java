package com.ksa.financing.infra.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for error message localization.
 *
 * <p>Services can register additional message basenames to extend
 * the default error messages provided by the SDK.</p>
 *
 * <pre>
 * ksa:
 *   error:
 *     additional-basenames:
 *       - errors/customer-errors
 *       - errors/loan-errors
 * </pre>
 */
@ConfigurationProperties(prefix = "ksa.error")
public class ErrorMessageProperties {

    private List<String> additionalBasenames = new ArrayList<>();

    public List<String> getAdditionalBasenames() {
        return additionalBasenames;
    }

    public void setAdditionalBasenames(List<String> additionalBasenames) {
        this.additionalBasenames = additionalBasenames != null ? additionalBasenames : new ArrayList<>();
    }
}
