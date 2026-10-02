package com.jimm0063.magi.document.utilities.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.jimm0063.magi.document.utilities.config.MagiProperties;
import com.jimm0063.magi.document.utilities.core.ModuleException;
import com.jimm0063.magi.document.utilities.core.ModuleRequest;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Builds a {@link ModuleRequest} from whatever Apple Shortcuts (or a browser) sends:
 * <ul>
 *   <li>a raw body of any type (text/plain, text/markdown, a file) — the body is the text</li>
 *   <li>flat JSON: {@code {"text": "...", "option": "..."}}</li>
 *   <li>a form (urlencoded or multipart) with a {@code text} field or a {@code file} part</li>
 * </ul>
 * Query parameters are always added as options; a {@code text} query parameter is used as text for GET.
 */
@Component
public class RequestReader {

    private final JsonMapper jsonMapper;
    private final MagiProperties properties;

    public RequestReader(JsonMapper jsonMapper, MagiProperties properties) {
        this.jsonMapper = jsonMapper;
        this.properties = properties;
    }

    public ModuleRequest read(HttpServletRequest request) throws IOException {
        Map<String, String> options = new HashMap<>();
        String text = null;
        String contentType = request.getContentType() == null ? "" : request.getContentType().toLowerCase();

        if (request instanceof MultipartHttpServletRequest multipart) {
            text = firstFile(multipart);
        } else if (contentType.startsWith(MediaType.APPLICATION_JSON_VALUE)) {
            text = readJson(request.getInputStream().readAllBytes(), options);
        } else if (!contentType.startsWith(MediaType.APPLICATION_FORM_URLENCODED_VALUE)) {
            byte[] body = request.getInputStream().readAllBytes();
            if (body.length > 0) {
                text = decode(body);
            }
        }

        // Query string, urlencoded and multipart fields all appear as request parameters.
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            if (entry.getValue().length > 0) {
                options.putIfAbsent(entry.getKey(), entry.getValue()[0]);
            }
        }
        options.remove("raw");
        options.remove("apiKey");
        String textParam = options.remove("text");
        if (text == null || text.isEmpty()) {
            text = textParam;
        }

        if (text != null && text.length() > properties.maxInputChars()) {
            throw new ModuleException("Text is too long (max " + properties.maxInputChars() + " characters)");
        }
        String baseUrl = ServletUriComponentsBuilder.fromContextPath(request).build().toUriString();
        return new ModuleRequest(text, options, baseUrl);
    }

    private String firstFile(MultipartHttpServletRequest multipart) throws IOException {
        MultipartFile file = multipart.getFile("file");
        if (file == null) {
            Iterator<String> names = multipart.getFileNames();
            file = names.hasNext() ? multipart.getFile(names.next()) : null;
        }
        return file == null || file.isEmpty() ? null : decode(file.getBytes());
    }

    private String readJson(byte[] body, Map<String, String> options) {
        if (body.length == 0) {
            return null;
        }
        Map<?, ?> json;
        try {
            json = jsonMapper.readValue(body, Map.class);
        } catch (JacksonException e) {
            throw new ModuleException("Body is not a valid JSON object");
        }
        String text = null;
        for (Map.Entry<?, ?> entry : json.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            String key = String.valueOf(entry.getKey());
            String value = String.valueOf(entry.getValue());
            if ("text".equals(key)) {
                text = value;
            } else {
                options.put(key, value);
            }
        }
        return text;
    }

    private static String decode(byte[] bytes) {
        String text = new String(bytes, StandardCharsets.UTF_8);
        return text.startsWith("﻿") ? text.substring(1) : text;
    }
}
