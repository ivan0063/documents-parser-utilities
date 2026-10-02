package com.jimm0063.magi.document.utilities.store;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import com.jimm0063.magi.document.utilities.config.MagiProperties;

/**
 * In-memory, self-expiring store for rendered documents. Nothing is persisted; a restart clears it.
 */
@Component
public class DocumentStore {

    private final SecureRandom random = new SecureRandom();
    private final Cache<String, RenderedDocument> cache;

    public DocumentStore(MagiProperties properties) {
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(properties.store().ttl())
                .maximumSize(properties.store().maxDocuments())
                .build();
    }

    public String newId() {
        byte[] bytes = new byte[9];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public void put(RenderedDocument document) {
        cache.put(document.id(), document);
    }

    public Optional<RenderedDocument> find(String id) {
        return Optional.ofNullable(cache.getIfPresent(id));
    }
}
