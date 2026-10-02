package com.jimm0063.magi.document.utilities.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.jimm0063.magi.document.utilities.config.MagiProperties;
import com.jimm0063.magi.document.utilities.core.ModuleResponse;

import tools.jackson.databind.json.JsonMapper;

/**
 * Optional protection for {@code /api/**}: when {@code magi.api-key} is set, requests must send it in
 * the {@code X-API-Key} header (or {@code apiKey} query parameter). Disabled when the key is empty.
 */
@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-API-Key";

    private final MagiProperties properties;
    private final JsonMapper jsonMapper;

    public ApiKeyFilter(MagiProperties properties, JsonMapper jsonMapper) {
        this.properties = properties;
        this.jsonMapper = jsonMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.apiKeyEnabled() || !request.getRequestURI().startsWith(request.getContextPath() + "/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        if (provided == null) {
            provided = request.getParameter("apiKey");
        }
        if (provided != null && MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8), properties.apiKey().getBytes(StandardCharsets.UTF_8))) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(response.getOutputStream(),
                ModuleResponse.error("core", "auth", "Invalid or missing API key (header " + HEADER + ")"));
    }
}
