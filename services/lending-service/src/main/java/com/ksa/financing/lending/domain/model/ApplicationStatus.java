package com.ksa.financing.lending.domain.model;

/**
 * Loan application status matching the UI stepper flow:
 * Basic Information → Add Bank Account → Checking Eligibility → Accept Offer → Sign Contract → Done
 */
public enum ApplicationStatus {

    // Phase 0: Application initiated (pre-qual done, "Apply Now" clicked)
    DRAFT,

    // Phase 1: Basic Information submitted (product, amount, purpose selected)
    BASIC_INFO_SUBMITTED,

    // Phase 2: Bank account
    BANK_ACCOUNT_PENDING,
    BANK_ACCOUNT_VERIFIED,

    // Phase 3: Eligibility check
    SIMAH_CONSENT_GIVEN,
    ELIGIBILITY_CHECKING,
    ELIGIBILITY_PASSED,

    // Phase 4: Offer
    OFFER_PRESENTED,
    OFFER_ACCEPTED,

    // Phase 5: Contract signing
    CONTRACT_PENDING,
    CONTRACT_SIGNING,
    OTP_VERIFICATION,
    IVR_VERIFICATION,
    CONTRACT_SIGNED,

    // Phase 6: Loan creation & disbursement
    LOAN_CREATING,
    DISBURSING,

    // Terminal states
    APPROVED,
    REJECTED,
    CANCELLED,
    EXPIRED,

    // BRD V1.8 Step 71: User can resume within 30 days if contract signing expired
    EXPIRED_RESUMABLE;

    public boolean canTransitionTo(ApplicationStatus target) {
        if (target == CANCELLED || target == EXPIRED) {
            // Can cancel/expire from any non-terminal state
            return !this.isTerminal();
        }

        return switch (this) {
            case DRAFT -> target == BASIC_INFO_SUBMITTED;
            case BASIC_INFO_SUBMITTED -> target == BANK_ACCOUNT_PENDING || target == REJECTED;
            case BANK_ACCOUNT_PENDING -> target == BANK_ACCOUNT_VERIFIED;
            case BANK_ACCOUNT_VERIFIED -> target == SIMAH_CONSENT_GIVEN;
            case SIMAH_CONSENT_GIVEN -> target == ELIGIBILITY_CHECKING;
            case ELIGIBILITY_CHECKING -> target == ELIGIBILITY_PASSED || target == REJECTED;
            case ELIGIBILITY_PASSED -> target == OFFER_PRESENTED;
            case OFFER_PRESENTED -> target == OFFER_ACCEPTED;
            case OFFER_ACCEPTED -> target == CONTRACT_PENDING;
            case CONTRACT_PENDING -> target == CONTRACT_SIGNING;
            case CONTRACT_SIGNING -> target == OTP_VERIFICATION;
            case OTP_VERIFICATION -> target == IVR_VERIFICATION || target == CONTRACT_SIGNING;
            case IVR_VERIFICATION -> target == CONTRACT_SIGNED || target == CONTRACT_SIGNING;
            case CONTRACT_SIGNED -> target == LOAN_CREATING;
            case LOAN_CREATING -> target == DISBURSING || target == REJECTED;
            case DISBURSING -> target == APPROVED || target == REJECTED;
            case EXPIRED_RESUMABLE -> target == CONTRACT_PENDING;
            case APPROVED, REJECTED, CANCELLED, EXPIRED -> false;
        };
    }

    public boolean isTerminal() {
        return this == APPROVED || this == REJECTED || this == CANCELLED || this == EXPIRED;
    }

    /**
     * Maps status to UI stepper index (1-5).
     */
    public int getStepperIndex() {
        return switch (this) {
            case DRAFT, BASIC_INFO_SUBMITTED -> 1;
            case BANK_ACCOUNT_PENDING, BANK_ACCOUNT_VERIFIED -> 2;
            case SIMAH_CONSENT_GIVEN, ELIGIBILITY_CHECKING, ELIGIBILITY_PASSED -> 3;
            case OFFER_PRESENTED, OFFER_ACCEPTED -> 4;
            case CONTRACT_PENDING, CONTRACT_SIGNING, OTP_VERIFICATION,
                 IVR_VERIFICATION, CONTRACT_SIGNED -> 5;
            case LOAN_CREATING, DISBURSING, APPROVED -> 5;
            case EXPIRED_RESUMABLE -> 5;
            case REJECTED, CANCELLED, EXPIRED -> 0;
        };
    }

    /**
     * Human-readable step name for the UI stepper bar.
     */
    public String getStepperLabel() {
        return switch (this) {
            case DRAFT, BASIC_INFO_SUBMITTED -> "Basic Information";
            case BANK_ACCOUNT_PENDING, BANK_ACCOUNT_VERIFIED -> "Add Bank Account";
            case SIMAH_CONSENT_GIVEN, ELIGIBILITY_CHECKING, ELIGIBILITY_PASSED -> "Checking Eligibility";
            case OFFER_PRESENTED, OFFER_ACCEPTED -> "Accept Offer";
            case CONTRACT_PENDING, CONTRACT_SIGNING, OTP_VERIFICATION,
                 IVR_VERIFICATION, CONTRACT_SIGNED,
                 LOAN_CREATING, DISBURSING, APPROVED -> "Sign Contract";
            case EXPIRED_RESUMABLE -> "Contract Expired (Resumable)";
            case REJECTED -> "Rejected";
            case CANCELLED -> "Cancelled";
            case EXPIRED -> "Expired";
        };
    }
}
