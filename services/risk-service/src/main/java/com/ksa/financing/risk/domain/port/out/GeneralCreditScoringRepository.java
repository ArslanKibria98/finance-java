package com.ksa.financing.risk.domain.port.out;

import com.ksa.financing.risk.domain.model.credit.CreditScoringCriteria;
import com.ksa.financing.risk.domain.model.credit.CustomerCreditScoreCurrent;
import com.ksa.financing.risk.domain.model.credit.CustomerCreditScoreSnapshot;
import com.ksa.financing.risk.domain.model.credit.GeneralScoringConfig;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for the GENERAL (product-agnostic, onboarding) credit scoring
 * setup, plus per-customer scoring history + cached current score.
 *
 * Distinct from {@link CreditScoringRepository} which owns the PRODUCT scoring
 * tables (product_credit_scoring_criteria / product_credit_scoring_rules).
 */
public interface GeneralCreditScoringRepository {

    // ---- Criteria + rules (general, tenant-level) ----

    List<CreditScoringCriteria> findAllCriteria(UUID tenantId);

    Optional<CreditScoringCriteria> findCriteriaById(UUID tenantId, UUID id);

    /** Bulk replace-all (kept for backward compatibility / bulk import). */
    void saveCriteria(UUID tenantId, List<CreditScoringCriteria> criteria);

    /** Insert ONE criterion + its rules. Returns the persisted criterion (with generated id). */
    CreditScoringCriteria saveSingleCriteria(UUID tenantId, CreditScoringCriteria criterion);

    /** Update ONE criterion + replace its rules. */
    CreditScoringCriteria updateSingleCriteria(UUID tenantId, UUID id, CreditScoringCriteria criterion);

    /** Delete ONE criterion (rules cascade). */
    void deleteSingleCriteria(UUID tenantId, UUID id);

    void deleteAllCriteria(UUID tenantId);

    // ---- Config (thresholds) ----

    Optional<GeneralScoringConfig> findConfig(UUID tenantId);

    GeneralScoringConfig saveConfig(GeneralScoringConfig config);

    // ---- Snapshots + current ----

    CustomerCreditScoreSnapshot saveSnapshot(CustomerCreditScoreSnapshot snapshot);

    List<CustomerCreditScoreSnapshot> findSnapshotsByCustomer(UUID tenantId, UUID customerId);

    Optional<CustomerCreditScoreCurrent> findCurrentByCustomer(UUID tenantId, UUID customerId);

    CustomerCreditScoreCurrent upsertCurrent(CustomerCreditScoreCurrent current);
}
