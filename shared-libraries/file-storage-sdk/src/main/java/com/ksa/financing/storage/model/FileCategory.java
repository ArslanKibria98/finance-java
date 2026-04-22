package com.ksa.financing.storage.model;

/**
 * Categories of files stored in the platform.
 * Used to organize objects in MinIO under a consistent path structure.
 *
 * Object key pattern: tenants/{tenantId}/{category}/{ownerId}/{uuid}.{ext}
 */
public enum FileCategory {

    PROFILE_PICTURE("profile-pictures"),
    KYC_DOCUMENT("kyc-documents"),
    INCOME_PROOF("income-proofs"),
    EMPLOYMENT_LETTER("employment-letters"),
    BANK_STATEMENT("bank-statements"),
    CONTRACT("contracts"),
    COLLATERAL_DOCUMENT("collateral-documents"),
    GENERIC("generic");

    private final String path;

    FileCategory(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
