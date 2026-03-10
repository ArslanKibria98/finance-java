package com.ksa.financing.globalprofile.application.mapper;

import com.ksa.financing.globalprofile.application.dto.GlobalProfileResponse;
import com.ksa.financing.globalprofile.application.dto.PiiAccessTokenResponse;
import com.ksa.financing.globalprofile.application.dto.RegionalProfileResponse;
import com.ksa.financing.globalprofile.domain.model.GlobalCustomer;
import com.ksa.financing.globalprofile.domain.model.PiiAccessToken;
import com.ksa.financing.globalprofile.domain.model.RegionalProfile;

public class GlobalProfileMapper {

    private GlobalProfileMapper() {}

    public static GlobalProfileResponse toResponse(GlobalCustomer gc) {
        return new GlobalProfileResponse(
            gc.getGlobalUid(),
            gc.getCustomerType() != null ? gc.getCustomerType().name() : null,
            gc.getPrimaryCountryCode(),
            gc.getGlobalKycStatus() != null ? gc.getGlobalKycStatus().name() : null,
            gc.getGlobalRiskGrade(),
            gc.isPepFlag(),
            gc.isSanctionsFlag(),
            gc.isFraudFlag(),
            gc.isActive(),
            gc.getCustomerSegment(),
            gc.getCreatedAt(),
            gc.getUpdatedAt()
        );
    }

    public static RegionalProfileResponse toResponse(RegionalProfile rp) {
        return new RegionalProfileResponse(
            rp.getRegionalProfileId(),
            rp.getGlobalUid(),
            rp.getCountryCode(),
            rp.getRegionalCifNumber(),
            rp.getRegionalKycStatus() != null ? rp.getRegionalKycStatus().name() : null,
            rp.getKycVerifiedAt(),
            rp.getKycExpiryDate(),
            rp.getPiiVaultRegion(),
            rp.getPiiVaultRecordId(),
            rp.isActive(),
            rp.getCreatedAt()
        );
    }

    public static PiiAccessTokenResponse toResponse(PiiAccessToken token) {
        return new PiiAccessTokenResponse(
            token.getTokenId(),
            token.getAccessToken(),
            token.getGlobalUid(),
            token.getAllowedFields(),
            token.getAccessPurpose(),
            token.getIssuedAt(),
            token.getExpiresAt()
        );
    }
}
