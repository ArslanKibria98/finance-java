package com.ksa.financing.fraud.domain.port.out;

public interface NameSimilarityPort {

    /**
     * Returns similarity score in [0.0, 1.0]. 1.0 = exact match.
     */
    double similarity(String a, String b);

    boolean matches(String a, String b, double threshold);
}
