package com.ksa.financing.risk.domain.model.credit;

/**
 * BRS-aligned credit decisioning outcome (LOS §5 Step 4).
 *
 * <ul>
 *   <li>{@link #AUTO_APPROVE} — Green: score ≥ green threshold</li>
 *   <li>{@link #REFER_MANUAL_REVIEW} — Amber: amber ≤ score &lt; green</li>
 *   <li>{@link #AUTO_REJECT} — Red: score &lt; amber threshold</li>
 * </ul>
 */
public enum CreditDecision {
    AUTO_APPROVE,
    REFER_MANUAL_REVIEW,
    AUTO_REJECT
}
