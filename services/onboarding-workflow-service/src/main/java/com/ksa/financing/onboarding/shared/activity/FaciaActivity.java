package com.ksa.financing.onboarding.shared.activity;

import com.ksa.financing.onboarding.shared.facia.FaciaContext;
import com.ksa.financing.onboarding.shared.facia.FaciaDocumentResult;
import com.ksa.financing.onboarding.shared.facia.FaciaFaceMatchResult;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface FaciaActivity {

    @ActivityMethod
    FaciaDocumentResult verifyDocument(String documentImageBase64);

    /**
     * Same as {@link #verifyDocument(String)} but propagates business context
     * (mobile, customerId, applicationId) through the middleware so Kibana can
     * link the call back to the customer.
     */
    @ActivityMethod
    FaciaDocumentResult verifyDocumentWithContext(String documentImageBase64, FaciaContext context);

    @ActivityMethod
    FaciaFaceMatchResult faceMatch(String selfieBase64, String documentImageBase64);

    /**
     * Same as {@link #faceMatch(String, String)} but propagates business context.
     */
    @ActivityMethod
    FaciaFaceMatchResult faceMatchWithContext(String selfieBase64, String documentImageBase64, FaciaContext context);
}
