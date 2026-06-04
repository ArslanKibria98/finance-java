package com.ksa.financing.onboarding.shared.sullis;

import java.io.Serializable;
import java.util.Map;

/**
 * Result of the Sullis document phase (create-session → start-attempt → upload-document).
 *
 * <p>{@code sessionId} + {@code attemptId} are carried back so the workflow can persist
 * them (keyed by workflowId) and reuse them in the later selfie/submit phase — without
 * them the selfie upload would not know which Sullis session it belongs to.</p>
 *
 * @param accepted     whether the document was uploaded + OCR-extracted successfully
 * @param sessionId    Sullis session id (store against the workflow)
 * @param attemptId    Sullis attempt id (store against the workflow)
 * @param uploadId     Sullis upload id for the document image
 * @param declineReason failure reason when {@code accepted == false}
 * @param extractedData OCR fields flattened to the keys the workflow / customer-service expect
 *                      (full_name, dob, document_number, expiry_date, nationality, ...)
 */
public record SullisDocumentResult(
        boolean accepted,
        String sessionId,
        String attemptId,
        String uploadId,
        String declineReason,
        Map<String, Object> extractedData
) implements Serializable {
}
