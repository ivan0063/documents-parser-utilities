package com.jimm0063.magi.document.utilities.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application settings under the {@code magi.*} prefix (see application.yml).
 */
@ConfigurationProperties("magi")
public record MagiProperties(String apiKey, int maxInputChars, Store store, Browser browser, Mermaid mermaid) {

    /** In-memory document cache. Nothing is ever written to disk. */
    public record Store(Duration ttl, int maxDocuments) {
    }

    /** Headless Chromium used to render diagrams and PDFs. */
    public record Browser(String executablePath, boolean warmup, Duration timeout, double deviceScaleFactor) {
    }

    public record Mermaid(String theme) {
    }

    public boolean apiKeyEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }
}
