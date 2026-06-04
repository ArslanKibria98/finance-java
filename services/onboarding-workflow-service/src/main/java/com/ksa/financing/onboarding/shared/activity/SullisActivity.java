package com.ksa.financing.onboarding.shared.activity;

import com.ksa.financing.onboarding.shared.sullis.SullisContext;
import com.ksa.financing.onboarding.shared.sullis.SullisDocumentResult;
import com.ksa.financing.onboarding.shared.sullis.SullisVerificationResult;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Temporal activity for the Sullis KYC flow (replaces {@link FaciaActivity} on the
 * foreign onboarding worker). Each method maps to one phase of the Sullis session:
 *
 * <ul>
 *   <li>{@link #verifyDocument} — create session + start attempt + upload document (OCR)</li>
 *   <li>{@link #submitVerification} — upload selfie + run the verification pipeline</li>
 * </ul>
 *
 * <p>The {@code sessionId}/{@code attemptId} returned by {@link #verifyDocument} must be
 * persisted by the workflow and passed back into {@link #submitVerification}.</p>
 */
@ActivityInterface
public interface SullisActivity {

    @ActivityMethod
    SullisDocumentResult verifyDocument(String documentImageBase64, String customerReference,
                                        String documentType, String existingSessionId,
                                        SullisContext context);

    @ActivityMethod
    SullisVerificationResult submitVerification(String selfieImageBase64, String sessionId,
                                                String attemptId, SullisContext context);
}
