package com.ksa.financing.infra.exception;

/**
 * Centralized error code constants for the KSA Islamic Financing Platform.
 *
 * <p>Naming convention: {@code DOMAIN.ENTITY.PROBLEM} (dot-separated).
 * Each code doubles as a message property key for i18n resolution.</p>
 *
 * <p>Services can define additional domain-specific codes following
 * the same naming convention.</p>
 */
public final class ErrorCodes {
    private ErrorCodes() {}

    // ── Common (cross-cutting) ──────────────────────────────────────

    public static final String VALIDATION_FAILED    = "COMMON.VALIDATION.FAILED";
    public static final String ACCESS_DENIED        = "COMMON.AUTH.ACCESS_DENIED";
    public static final String INVALID_CREDENTIALS  = "COMMON.AUTH.INVALID_CREDENTIALS";
    public static final String NOT_FOUND            = "COMMON.RESOURCE.NOT_FOUND";
    public static final String CONFLICT             = "COMMON.RESOURCE.CONFLICT";
    public static final String BAD_REQUEST          = "COMMON.REQUEST.BAD_REQUEST";
    public static final String INTERNAL_ERROR       = "COMMON.SYSTEM.INTERNAL_ERROR";
    public static final String TECHNICAL_ERROR      = "COMMON.SYSTEM.TECHNICAL_ERROR";

    // ── Customer ────────────────────────────────────────────────────

    public static final class Customer {
        private Customer() {}
        public static final String ALREADY_EXISTS      = "CUSTOMER.PROFILE.ALREADY_EXISTS";
        public static final String NOT_FOUND           = "CUSTOMER.PROFILE.NOT_FOUND";
        public static final String INVALID_NID         = "CUSTOMER.NID.INVALID";
        public static final String CIF_GENERATION_FAIL = "CUSTOMER.CIF.GENERATION_FAILED";
        public static final String DUPLICATE_CODE      = "CUSTOMER.REFERENCE_DATA.DUPLICATE_CODE";
    }

    // ── KYC ─────────────────────────────────────────────────────────

    public static final class Kyc {
        private Kyc() {}
        public static final String VERIFICATION_FAILED  = "KYC.VERIFICATION.FAILED";
        public static final String NAFATH_TIMEOUT       = "KYC.NAFATH.TIMEOUT";
        public static final String YAKEEN_UNAVAILABLE   = "KYC.YAKEEN.UNAVAILABLE";
        public static final String SIMAH_UNAVAILABLE    = "KYC.SIMAH.UNAVAILABLE";
        public static final String OTP_EXPIRED          = "KYC.OTP.EXPIRED";
        public static final String OTP_INVALID          = "KYC.OTP.INVALID";
        public static final String OTP_REQUEST_NOT_FOUND = "KYC.OTP.REQUEST_NOT_FOUND";
        public static final String OTP_ALREADY_PROCESSED = "KYC.OTP.ALREADY_PROCESSED";
        public static final String OTP_MAX_ATTEMPTS      = "KYC.OTP.MAX_ATTEMPTS_EXCEEDED";
    }

    // ── Wallet ──────────────────────────────────────────────────────

    public static final class Wallet {
        private Wallet() {}
        public static final String NOT_FOUND           = "WALLET.ACCOUNT.NOT_FOUND";
        public static final String NOT_ACTIVE          = "WALLET.STATUS.NOT_ACTIVE";
        public static final String LIMIT_EXCEEDED      = "WALLET.TOPUP.LIMIT_EXCEEDED";
        public static final String INSUFFICIENT_FUNDS  = "WALLET.BALANCE.INSUFFICIENT";
    }

    // ── Risk ────────────────────────────────────────────────────────

    public static final class Risk {
        private Risk() {}
        public static final String SCORE_TOO_HIGH    = "RISK.SCORE.TOO_HIGH";
        public static final String AML_FLAGGED       = "RISK.AML.FLAGGED";
        public static final String SANCTIONS_MATCH   = "RISK.SANCTIONS.MATCH";
        public static final String VELOCITY_EXCEEDED = "RISK.VELOCITY.EXCEEDED";

        // Blacklist guard codes — emitted by SDK middleware on pre-auth checks
        public static final String BLACKLIST_NID     = "RISK.BLACKLIST.NID_BLOCKED";
        public static final String BLACKLIST_MOBILE  = "RISK.BLACKLIST.MOBILE_BLOCKED";
        public static final String BLACKLIST_DEVICE  = "RISK.BLACKLIST.DEVICE_BLOCKED";
        public static final String BLACKLIST_USER    = "RISK.BLACKLIST.USER_BLOCKED";
        public static final String BLACKLIST_IP      = "RISK.BLACKLIST.IP_BLOCKED";
        public static final String BLACKLIST_GENERIC = "RISK.BLACKLIST.BLOCKED";
    }

    // ── Loan ────────────────────────────────────────────────────────

    public static final class Loan {
        private Loan() {}
        public static final String NOT_FOUND         = "LOAN.APPLICATION.NOT_FOUND";
        public static final String INVALID_AMOUNT    = "LOAN.AMOUNT.INVALID";
        public static final String LIMIT_EXCEEDED    = "LOAN.AMOUNT.LIMIT_EXCEEDED";
        public static final String ALREADY_DISBURSED = "LOAN.STATUS.ALREADY_DISBURSED";
    }

    // ── Identity ────────────────────────────────────────────────────

    public static final class Identity {
        private Identity() {}
        public static final String USER_NOT_FOUND       = "IDENTITY.USER.NOT_FOUND";
        public static final String TOKEN_EXPIRED        = "IDENTITY.TOKEN.EXPIRED";
        public static final String SESSION_INVALID      = "IDENTITY.SESSION.INVALID";
        public static final String PIN_INVALID          = "IDENTITY.PIN.INVALID";
        public static final String PIN_NOT_SET          = "IDENTITY.PIN.NOT_SET";
        public static final String ACCOUNT_INACTIVE     = "IDENTITY.ACCOUNT.INACTIVE";
        public static final String ROLE_NOT_FOUND       = "IDENTITY.ROLE.NOT_FOUND";
        public static final String ROLE_DUPLICATE       = "IDENTITY.ROLE.DUPLICATE";
        public static final String ROLE_SYSTEM_IMMUTABLE = "IDENTITY.ROLE.SYSTEM_IMMUTABLE";
        public static final String PERMISSION_NOT_FOUND = "IDENTITY.PERMISSION.NOT_FOUND";
        public static final String PERMISSION_DUPLICATE = "IDENTITY.PERMISSION.DUPLICATE";
        public static final String POLICY_ALREADY_EXISTS = "IDENTITY.POLICY.ALREADY_EXISTS";
        public static final String POLICY_NOT_FOUND     = "IDENTITY.POLICY.NOT_FOUND";
        public static final String AUTHORIZATION_DENIED = "IDENTITY.AUTHORIZATION.DENIED";
        public static final String EMPLOYEE_NOT_FOUND  = "IDENTITY.EMPLOYEE.NOT_FOUND";
        public static final String EMPLOYEE_DUPLICATE  = "IDENTITY.EMPLOYEE.DUPLICATE_EMAIL";
        public static final String PASSCODE_MISMATCH   = "IDENTITY.PASSCODE.MISMATCH";
        public static final String RESET_FAILED        = "IDENTITY.PASSCODE.RESET_FAILED";
    }

    // ── Product ────────────────────────────────────────────────────

    public static final class Product {
        private Product() {}
        public static final String NOT_FOUND              = "PRODUCT.CATALOG.NOT_FOUND";
        public static final String DUPLICATE_CODE         = "PRODUCT.CATALOG.DUPLICATE_CODE";
        public static final String CATEGORY_NOT_FOUND     = "PRODUCT.CATEGORY.NOT_FOUND";
        public static final String TEMPLATE_NOT_FOUND     = "PRODUCT.TEMPLATE.NOT_FOUND";
        public static final String INVALID_TRANSITION     = "PRODUCT.STATUS.INVALID_TRANSITION";
        public static final String WIZARD_INCOMPLETE      = "PRODUCT.WIZARD.INCOMPLETE";
        public static final String PARTNER_DUPLICATE      = "PRODUCT.PARTNER.DUPLICATE";
        public static final String PARTNER_NOT_FOUND      = "PRODUCT.PARTNER.NOT_FOUND";
        public static final String DOCUMENT_NOT_FOUND     = "PRODUCT.DOCUMENT.NOT_FOUND";
        public static final String CONFIG_NOT_FOUND       = "PRODUCT.CONFIG.NOT_FOUND";
        public static final String HAS_ACTIVE_LOANS       = "PRODUCT.DELETE.HAS_ACTIVE_LOANS";
    }

    // ── Onboarding ─────────────────────────────────────────────────

    public static final class Onboarding {
        private Onboarding() {}
        public static final String EMAIL_ALREADY_REGISTERED   = "ONBOARDING.EMAIL.ALREADY_REGISTERED";
        public static final String MOBILE_ALREADY_REGISTERED  = "ONBOARDING.MOBILE.ALREADY_REGISTERED";
        public static final String ACCOUNT_ALREADY_REGISTERED = "ONBOARDING.ACCOUNT.ALREADY_REGISTERED";
    }

    // ── Collections ────────────────────────────────────────────────

    public static final class Collections {
        private Collections() {}
        public static final String WAIVER_NOT_ALLOWED = "COLLECTIONS.PENALTY.WAIVER_NOT_ALLOWED";
        public static final String WAIVER_LIMIT_EXCEEDED = "COLLECTIONS.PENALTY.WAIVER_LIMIT_EXCEEDED";
        public static final String NO_PENALTY_TO_WAIVE = "COLLECTIONS.PENALTY.NONE_REMAINING";
        public static final String SCHEDULE_NOT_SEEDED = "COLLECTIONS.SCHEDULE.NOT_SEEDED";
    }
}
