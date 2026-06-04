package com.ksa.financing.middleware.infrastructure.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Renders a stored JSON request template against a caller-supplied flat field map,
 * so callers send a SIMPLE request and the middleware produces the EXACT provider body.
 *
 * <p>Placeholder syntax inside template string values:</p>
 * <ul>
 *   <li><b>Exact</b> — a string value that is exactly {@code "${field}"} or {@code "${field:default}"}
 *       is replaced by the caller's value <i>preserving its JSON type</i> (a number stays a number).
 *       If the field is absent and a default is given, the default (a string) is used; if absent and
 *       no default, the property is <b>omitted</b> from the output.</li>
 *   <li><b>Embedded</b> — a string containing {@code ...${field}...} has each token replaced by the
 *       caller's value as text (or default, or empty).</li>
 * </ul>
 *
 * <p>Everything else in the template (objects, arrays, literal numbers/strings) is copied verbatim —
 * that is where the provider's fixed structure (addresses, scheme codes, priorities…) lives, so the
 * caller never has to send it. Provider-agnostic; driven entirely by the per-API template stored in
 * {@code provider_apis.request_template}.</p>
 */
public final class JsonTemplateRenderer {

    private JsonTemplateRenderer() {}

    /** Sentinel returned for an unresolved exact placeholder → the owning property is dropped. */
    private static final MissingNode DROP = MissingNode.getInstance();

    private static final Pattern EXACT = Pattern.compile("^\\$\\{([^:}]+)(?::([^}]*))?}$");
    private static final Pattern TOKEN = Pattern.compile("\\$\\{([^:}]+)(?::([^}]*))?}");

    public static JsonNode render(JsonNode template, JsonNode data, ObjectMapper om) {
        if (template == null) return om.createObjectNode();
        if (data == null || !data.isObject()) data = om.createObjectNode();
        JsonNode out = renderNode(template, data, om);
        return out == DROP ? om.createObjectNode() : out;
    }

    private static JsonNode renderNode(JsonNode node, JsonNode data, ObjectMapper om) {
        if (node.isObject()) {
            ObjectNode result = om.createObjectNode();
            node.fields().forEachRemaining(e -> {
                JsonNode rendered = renderNode(e.getValue(), data, om);
                if (rendered != DROP) result.set(e.getKey(), rendered);
            });
            return result;
        }
        if (node.isArray()) {
            ArrayNode result = om.createArrayNode();
            for (JsonNode el : node) {
                JsonNode rendered = renderNode(el, data, om);
                if (rendered != DROP) result.add(rendered);
            }
            return result;
        }
        if (node.isTextual()) {
            return renderText(node.asText(), data, om);
        }
        return node; // numbers, booleans, nulls — verbatim
    }

    private static JsonNode renderText(String text, JsonNode data, ObjectMapper om) {
        Matcher exact = EXACT.matcher(text);
        if (exact.matches()) {
            String field = exact.group(1).trim();
            String def = exact.group(2); // may be null
            JsonNode value = data.get(field);
            if (value != null && !value.isNull()) return value;       // typed (number stays number)
            if (def != null) return TextNode.valueOf(def);            // string default
            return DROP;                                              // omit the property
        }
        // embedded tokens — string interpolation
        Matcher m = TOKEN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String field = m.group(1).trim();
            String def = m.group(2);
            JsonNode value = data.get(field);
            String replacement = (value != null && !value.isNull())
                    ? value.asText()
                    : (def != null ? def : "");
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return TextNode.valueOf(sb.toString());
    }
}
