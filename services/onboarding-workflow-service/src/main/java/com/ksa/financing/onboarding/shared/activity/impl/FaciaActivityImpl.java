package com.ksa.financing.onboarding.shared.activity.impl;

import com.ksa.financing.onboarding.shared.activity.FaciaActivity;
import com.ksa.financing.onboarding.shared.audit.OnboardingSessionRecorder;
import com.ksa.financing.onboarding.shared.audit.OnboardingSessionRecorder.RefType;
import com.ksa.financing.onboarding.shared.document.DocumentStorageClient;
import com.ksa.financing.onboarding.shared.facia.FaciaClient;
import com.ksa.financing.onboarding.shared.facia.FaciaContext;
import com.ksa.financing.onboarding.shared.facia.FaciaDocumentResult;
import com.ksa.financing.onboarding.shared.facia.FaciaFaceMatchResult;
import io.temporal.activity.Activity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class FaciaActivityImpl implements FaciaActivity {

    private final FaciaClient faciaClient;
    private final OnboardingSessionRecorder recorder;
    private final DocumentStorageClient documentStorage;
    private final String defaultTenantId;

    public FaciaActivityImpl(FaciaClient faciaClient,
                             OnboardingSessionRecorder recorder,
                             DocumentStorageClient documentStorage,
                             String defaultTenantId) {
        this.faciaClient = faciaClient;
        this.recorder = recorder;
        this.documentStorage = documentStorage;
        this.defaultTenantId = defaultTenantId;
    }

    @Override
    public FaciaDocumentResult verifyDocument(String documentImageBase64) {
        return runVerifyDocument(documentImageBase64, FaciaContext.empty());
    }

    @Override
    public FaciaDocumentResult verifyDocumentWithContext(String documentImageBase64, FaciaContext context) {
        return runVerifyDocument(documentImageBase64, context == null ? FaciaContext.empty() : context);
    }

    private FaciaDocumentResult runVerifyDocument(String documentImageBase64, FaciaContext context) {
        String workflowId = currentWorkflowId();
        FaciaDocumentResult result = faciaClient.verifyDocument(documentImageBase64, context);
        if (result.referenceId() != null) {
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("event", result.event());
            meta.put("accepted", result.accepted());
            if (result.declineReason() != null) meta.put("declineReason", result.declineReason());
            recorder.recordExternalRef(workflowId, RefType.FACIA_DOC, result.referenceId(), meta);
        }
        recorder.recordStep(workflowId,
                result.accepted() ? "FACIA_DOC_ACCEPTED" : "FACIA_DOC_DECLINED",
                Map.of("referenceId", String.valueOf(result.referenceId())),
                result.accepted() ? null : result.declineReason());

        // Persist the raw image (encrypted) only when Facia accepts. Failures are
        // logged but never propagate — image archive is a side-effect, not part of
        // the verification contract.
        if (result.accepted() && documentImageBase64 != null) {
            String docNumber = result.extractedData() != null
                    ? stringValue(result.extractedData().get("document_number")) : null;
            String selectedType = result.extractedData() != null
                    ? stringValue(result.extractedData().get("selected_type")) : null;
            String kind = mapKind(selectedType);
            String sourceFlow = deriveSourceFlow(workflowId);
            documentStorage.store(new DocumentStorageClient.StoreInput(
                    parseTenant(defaultTenantId),
                    null,
                    workflowId,
                    kind,
                    sourceFlow,
                    docNumber,
                    result.referenceId(),
                    "image/jpeg",
                    documentImageBase64,
                    workflowId + ":doc"));
        }
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

    @Override
    public FaciaFaceMatchResult faceMatch(String selfieBase64, String documentImageBase64) {
        return runFaceMatch(selfieBase64, documentImageBase64, FaciaContext.empty());
    }

    @Override
    public FaciaFaceMatchResult faceMatchWithContext(String selfieBase64, String documentImageBase64, FaciaContext context) {
        return runFaceMatch(selfieBase64, documentImageBase64, context == null ? FaciaContext.empty() : context);
    }

    private FaciaFaceMatchResult runFaceMatch(String selfieBase64, String documentImageBase64, FaciaContext context) {
        String workflowId = currentWorkflowId();
        FaciaFaceMatchResult result = faciaClient.faceMatch(selfieBase64, documentImageBase64, context);
        if (result.referenceId() != null) {
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("similarityScore", result.similarityScore());
            meta.put("similarityStatus", result.similarityStatus());
            meta.put("match", result.match());
            recorder.recordExternalRef(workflowId, RefType.FACIA_SELFIE, result.referenceId(), meta);
        }
        recorder.recordStep(workflowId,
                result.match() ? "FACIA_FACE_MATCH_OK" : "FACIA_FACE_MATCH_FAILED",
                Map.of("score", String.valueOf(result.similarityScore())),
                result.match() ? null : result.failureReason());
        return result;
    }

    private static String currentWorkflowId() {
        try {
            return Activity.getExecutionContext().getInfo().getWorkflowId();
        } catch (Exception e) {
            return "unknown-workflow";
        }
    }
}
