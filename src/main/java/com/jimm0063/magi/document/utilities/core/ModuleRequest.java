package com.jimm0063.magi.document.utilities.core;

import java.util.Map;

/**
 * Input for a module action.
 *
 * @param text    the main text (may be empty)
 * @param options flat string options taken from the query string, JSON body or form fields
 * @param baseUrl scheme://host:port the client used to reach the service, for building links
 */
public record ModuleRequest(String text, Map<String, String> options, String baseUrl) {

    public ModuleRequest {
        text = text == null ? "" : text;
        options = options == null ? Map.of() : Map.copyOf(options);
        baseUrl = baseUrl == null ? "" : baseUrl;
    }

    public String option(String name, String defaultValue) {
        String value = options.get(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public String requireText() {
        if (text.isBlank()) {
            throw new ModuleException("Text is empty");
        }
        return text;
    }
}
