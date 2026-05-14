package com.ksa.financing.infra.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Masks PII inside JSON bodies, plain-text bodies, and HTTP header maps.
 * Required for PDPL / SAMA — request/response payloads MUST NOT leak NID,
 * mobile, card, IBAN, OTP, password, or bearer tokens to Elasticsearch.
 */
@Slf4j
@RequiredArgsConstructor
public class PiiMasker {

    private static final String MASK = "***MASKED***";
    private static final String REDACTED = "[REDACTED]";

    // 10-digit Saudi NID / Iqama (no longer/shorter to avoid order numbers, etc.)
    private static final Pattern NID_PATTERN = Pattern.compile("\\b[12]\\d{9}\\b");
    // Saudi mobile: +9665XXXXXXXX or 9665... or 05XXXXXXXX
    private static final Pattern MOBILE_PATTERN = Pattern.compile("\\b(?:\\+?966|0)5\\d{8}\\b");
    // Card numbers (13-19 digits, optionally separated)
    private static final Pattern CARD_PATTERN = Pattern.compile("\\b(?:\\d[ -]?){13,19}\\b");
    // IBAN (Saudi: SA + 22 digits)
    private static final Pattern IBAN_PATTERN = Pattern.compile("\\bSA\\d{22}\\b", Pattern.CASE_INSENSITIVE);
    // Bearer tokens
    private static final Pattern BEARER_PATTERN = Pattern.compile("Bearer\\s+[A-Za-z0-9._\\-]+", Pattern.CASE_INSENSITIVE);
    // Email
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}\\b");

    private final ObjectMapper objectMapper;
    private final ApiAuditProperties properties;

    public Map<String, String> maskHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return headers;
        }
        List<String> denyList = properties.getMaskHeaders().stream()
                .map(s -> s.toLowerCase(Locale.ROOT))
                .toList();
        int cap = properties.getMaxHeaderValueLength();

        java.util.LinkedHashMap<String, String> out = new java.util.LinkedHashMap<>();
        headers.forEach((k, v) -> {
            String key = k == null ? "" : k.toLowerCase(Locale.ROOT);
            String value = v == null ? "" : v;
            if (denyList.contains(key)) {
                out.put(k, REDACTED);
            } else if (value.length() > cap) {
                out.put(k, value.substring(0, cap) + "...[truncated]");
            } else {
                out.put(k, value);
            }
        });
        return out;
    }

    public String maskBody(String body, String contentType) {
        if (body == null || body.isEmpty()) {
            return body;
        }
        if (contentType != null && contentType.toLowerCase(Locale.ROOT).contains("json")) {
            String masked = tryMaskJson(body);
            return maskPatterns(masked);
        }
        return maskPatterns(body);
    }

    private String tryMaskJson(String body) {
        try {
            JsonNode node = objectMapper.readTree(body);
            maskJsonInPlace(node);
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            // Not valid JSON, fall back to regex masking
            return body;
        }
    }

    private void maskJsonInPlace(JsonNode node) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = obj.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String field = entry.getKey().toLowerCase(Locale.ROOT);
                if (shouldMaskField(field)) {
                    obj.set(entry.getKey(), TextNode.valueOf(MASK));
                } else {
                    maskJsonInPlace(entry.getValue());
                }
            }
        } else if (node.isArray()) {
            ArrayNode arr = (ArrayNode) node;
            for (int i = 0; i < arr.size(); i++) {
                maskJsonInPlace(arr.get(i));
            }
        }
    }

    private boolean shouldMaskField(String fieldNameLower) {
        for (String pattern : properties.getMaskFields()) {
            String p = pattern.toLowerCase(Locale.ROOT);
            if (fieldNameLower.contains(p)) {
                return true;
            }
        }
        return false;
    }

    private String maskPatterns(String text) {
        if (text == null) return null;
        String out = BEARER_PATTERN.matcher(text).replaceAll("Bearer " + REDACTED);
        out = NID_PATTERN.matcher(out).replaceAll(MASK);
        out = IBAN_PATTERN.matcher(out).replaceAll(MASK);
        out = CARD_PATTERN.matcher(out).replaceAll(MASK);
        out = MOBILE_PATTERN.matcher(out).replaceAll(MASK);
        out = EMAIL_PATTERN.matcher(out).replaceAll(this::maskEmail);
        return out;
    }

    private String maskEmail(java.util.regex.MatchResult m) {
        String email = m.group();
        int at = email.indexOf('@');
        if (at <= 1) return MASK;
        return email.charAt(0) + "***" + email.substring(at);
    }

    public String truncate(String body) {
        if (body == null) return null;
        int cap = properties.getMaxBodySizeBytes();
        if (body.length() <= cap) return body;
        return body.substring(0, cap) + "...[truncated " + (body.length() - cap) + " bytes]";
    }
}
