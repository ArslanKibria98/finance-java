package com.ksa.financing.domain.enums;

/**
 * Customer lifecycle status.
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public enum CustomerStatus {
    /** Account created, basic info provided */
    LEAD,

    /** KYC verification in progress */
    PROSPECT,

    /** KYC verified, eligible for products */
    QUALIFIED,

    /** Has submitted loan application */
    APPLICANT,

    /** Has active financial product */
    ACTIVE,

    /** All products settled, inactive > 90 days */
    DORMANT,

    /** No activity > 12 months post-dormancy */
    CHURNED,

    /** Blocked due to fraud/compliance */
    BLOCKED
}
