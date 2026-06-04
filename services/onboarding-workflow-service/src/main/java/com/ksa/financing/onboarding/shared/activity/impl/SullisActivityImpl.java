package com.ksa.financing.onboarding.shared.activity.impl;

import com.ksa.financing.onboarding.shared.activity.SullisActivity;
import com.ksa.financing.onboarding.shared.audit.OnboardingSessionRecorder;
import com.ksa.financing.onboarding.shared.audit.OnboardingSessionRecorder.RefType;
import com.ksa.financing.onboarding.shared.document.DocumentStorageClient;
import com.ksa.financing.onboarding.shared.sullis.SullisClient;
import com.ksa.financing.onboarding.shared.sullis.SullisContext;
import com.ksa.financing.onboarding.shared.sullis.SullisDocumentResult;
import com.ksa.financing.onboarding.shared.sullis.SullisVerificationResult;
import io.temporal.activity.Activity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sullis activity implementation. Mirrors {@link FaciaActivityImpl}: delegates the HTTP
 * work to {@link SullisClient}, records audit steps + external refs, and archives the
 * accepted document image (encrypted, best-effort) — but drives the Sullis session flow.
 */
public class SullisActivityImpl implements SullisActivity {

    private final SullisClient sullisClient;
    private final OnboardingSessionRecorder recorder;
    private final DocumentStorageClient documentStorage;
    private final String defaultTenantId;

    public SullisActivityImpl(SullisClient sullisClient,
                              OnboardingSessionRecorder recorder,
                              DocumentStorageClient documentStorage,
                              String defaultTenantId) {
        this.sullisClient = sullisClient;
        this.recorder = recorder;
        this.documentStorage = documentStorage;
        this.defaultTenantId = defaultTenantId;
    }

    @Override
    public SullisDocumentResult verifyDocument(String documentImageBase64, String customerReference,
                                               String documentType, String existingSessionId,
                                               SullisContext context) {
        String workflowId = currentWorkflowId();
        SullisContext ctx = context == null ? SullisContext.empty() : context;
        String reference = customerReference != null ? customerReference : workflowId;

        SullisDocumentResult result = sullisClient.verifyDocument(
                documentImageBase64, reference, documentType, existingSessionId, ctx);

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("sessionId", result.sessionId());
        meta.put("attemptId", result.attemptId());
        meta.put("accepted", result.accepted());
        if (result.declineReason() != null) meta.put("declineReason", result.declineReason());
        if (result.uploadId() != null) {
            recorder.recordExternalRef(workflowId, RefType.FACIA_DOC, result.uploadId(), meta);
        }
        recorder.recordStep(workflowId,
                result.accepted() ? "SULLIS_DOC_ACCEPTED" : "SULLIS_DOC_DECLINED",
                Map.of("sessionId", String.valueOf(result.sessionId()),
                        "attemptId", String.valueOf(result.attemptId())),
                result.accepted() ? null : result.declineReason());

        if (result.accepted() && documentImageBase64 != null) {
            String docNumber = result.extractedData() != null
                    ? stringValue(result.extractedData().get("document_number")) : null;
            String selectedType = result.extractedData() != null
                    ? stringValue(result.extractedData().get("selected_type")) : null;
            String kind = mapKind(selectedType);
            String sourceFlow = deriveSourceFlow(workflowId);
            try {
                documentStorage.store(new DocumentStorageClient.StoreInput(
                        parseTenant(defaultTenantId), null, workflowId, kind, sourceFlow,
                        docNumber, result.uploadId(), "image/jpeg", documentImageBase64,
                        workflowId + ":doc"));
            } catch (Exception e) {
                // image archive is a side-effect, never block the verification contract
            }
        }
        return result;
    }

    @Override
    public SullisVerificationResult submitVerification(String selfieImageBase64, String sessionId,
                                                       String attemptId, SullisContext context) {
        String workflowId = currentWorkflowId();
        SullisContext ctx = context == null ? SullisContext.empty() : context;

        SullisVerificationResult result = sullisClient.submitVerification(
                selfieImageBase64, sessionId, attemptId, ctx);

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("outcome", result.outcome());
        meta.put("faceMatchScore", result.faceMatchScore());
        meta.put("riskScore", result.riskScore());
        meta.put("approved", result.approved());
        recorder.recordExternalRef(workflowId, RefType.FACIA_SELFIE,
                attemptId != null ? attemptId : String.valueOf(sessionId), meta);
        recorder.recordStep(workflowId,
                result.approved() ? "SULLIS_VERIFY_APPROVED" : "SULLIS_VERIFY_DECLINED",
                Map.of("outcome", String.valueOf(result.outcome()),
                        "score", String.valueOf(result.faceMatchScore())),
                result.approved() ? null : result.reason());
        return result;
    }

    private static String mapKind(String selectedType) {
        if (selectedType == null) return "OTHER";
        String s = selectedType.toLowerCase();
        if (s.contains("passport")) return "PASSPORT";
        if (s.contains("driving") || s.contains("license") || s.contains("licence")) return "DRIVING_LICENCE";
        if (s.contains("residence") || s.contains("iqama") || s.contains("permit")) return "RESIDENCE_PERMIT";
        if (s.contains("id")) return "ID_CARD";
        return "OTHER";
    }

    private static String deriveSourceFlow(String workflowId) {
        if (workflowId == null) return "OTHER";
        if (workflowId.startsWith("canada-")) return "CANADA";
        if (workflowId.startsWith("foreign-")) return "FOREIGN";
        if (workflowId.startsWith("guest-")) return "GUEST";
        if (workflowId.startsWith("customer-onboarding-")) return "KSA";
        return "OTHER";
    }

    private static UUID parseTenant(String tenant) {
        try { return UUID.fromString(tenant); } catch (Exception e) { return null; }
    }

    private static String stringValue(Object o) { return o == null ? null : o.toString(); }

    private static String currentWorkflowId() {
        try {
            return Activity.getExecutionContext().getInfo().getWorkflowId();
        } catch (Exception e) {
            return "unknown-workflow";
        }
    }
}
