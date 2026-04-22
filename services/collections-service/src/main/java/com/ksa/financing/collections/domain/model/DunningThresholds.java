package com.ksa.financing.collections.domain.model;

/**
 * Immutable value object — DPD thresholds defining stage boundaries.
 * All values are inclusive lower bounds on days-past-due.
 *
 * Invariants:
 *   soft < hard < legal < writeOff
 *   preDueDaysBefore >= 0
 *   gracePeriodDays >= 0
 */
public record DunningThresholds(
        int preDueDaysBefore,
        int gracePeriodDays,
        int softCollectionDpd,
        int hardCollectionDpd,
        int legalDpd,
        int writeOffDpd
) {

    public DunningThresholds {
        if (preDueDaysBefore < 0)
            throw new IllegalArgumentException("preDueDaysBefore cannot be negative");
        if (gracePeriodDays < 0)
            throw new IllegalArgumentException("gracePeriodDays cannot be negative");
        if (softCollectionDpd < 1)
            throw new IllegalArgumentException("softCollectionDpd must be >= 1");
        if (!(softCollectionDpd < hardCollectionDpd
                && hardCollectionDpd < legalDpd
                && legalDpd < writeOffDpd)) {
            throw new IllegalArgumentException(
                    "Thresholds must be strictly ascending: soft < hard < legal < writeOff");
        }
    }

    /** Given a current DPD, return the stage the loan belongs to. */
    public DunningStage stageFor(int dpd) {
        if (dpd >= writeOffDpd)          return DunningStage.WRITE_OFF;
        if (dpd >= legalDpd)             return DunningStage.LEGAL;
        if (dpd >= hardCollectionDpd)    return DunningStage.HARD_COLLECTION;
        if (dpd >= softCollectionDpd)    return DunningStage.SOFT_COLLECTION;
        if (dpd > gracePeriodDays)       return DunningStage.SOFT_COLLECTION;
        if (dpd > 0)                     return DunningStage.GRACE_PERIOD;
        if (dpd == 0)                    return DunningStage.DUE_DATE_REMINDER;
        return DunningStage.PRE_DUE_REMINDER;
    }

    public static DunningThresholds defaults() {
        return new DunningThresholds(3, 10, 11, 31, 91, 181);
    }
}
