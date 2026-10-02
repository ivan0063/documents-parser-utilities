package com.jimm0063.magi.document.utilities.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The single response shape of the API, designed to be read easily from Apple Shortcuts.
 *
 * <p>It is always <b>flat</b>: one level of keys whose values are strings, numbers, booleans or lists of
 * strings. The base fields {@code success}, {@code module}, {@code action}, {@code result} and
 * {@code message} are always present; extra fields are added with {@link #with(String, Object)}, which
 * rejects anything that would make the response nested.
 */
public final class ModuleResponse {

    private static final List<String> BASE_FIELDS = List.of("success", "module", "action", "result", "message");

    private final Map<String, Object> fields = new LinkedHashMap<>();

    private ModuleResponse(boolean success, String module, String action, String result, String message) {
        fields.put("success", success);
        fields.put("module", nullToEmpty(module));
        fields.put("action", nullToEmpty(action));
        fields.put("result", nullToEmpty(result));
        fields.put("message", nullToEmpty(message));
    }

    public static ModuleResponse ok(String module, String action, String result) {
        return new ModuleResponse(true, module, action, result, "OK");
    }

    public static ModuleResponse error(String module, String action, String message) {
        return new ModuleResponse(false, module, action, "", message);
    }

    /**
     * Adds a top-level field. Allowed values: String, Number, Boolean or a List of Strings.
     */
    public ModuleResponse with(String key, Object value) {
        if (BASE_FIELDS.contains(key)) {
            throw new IllegalArgumentException("'" + key + "' is a base field; use message(...) or the factories");
        }
        fields.put(key, requireFlat(key, value));
        return this;
    }

    /** Convenience for list results: sets {@code items} and {@code count}. */
    public ModuleResponse items(List<String> items) {
        with("items", items);
        return with("count", items.size());
    }

    public ModuleResponse message(String message) {
        fields.put("message", nullToEmpty(message));
        return this;
    }

    public boolean success() {
        return (Boolean) fields.get("success");
    }

    public String result() {
        return (String) fields.get("result");
    }

    public Object get(String key) {
        return fields.get(key);
    }

    @JsonValue
    public Map<String, Object> asMap() {
        return Collections.unmodifiableMap(fields);
    }

    private static Object requireFlat(String key, Object value) {
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof List<?> list && list.stream().allMatch(String.class::isInstance)) {
            return List.copyOf(list);
        }
        throw new IllegalArgumentException("Field '" + key + "' must be a String, Number, Boolean or List<String>");
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
