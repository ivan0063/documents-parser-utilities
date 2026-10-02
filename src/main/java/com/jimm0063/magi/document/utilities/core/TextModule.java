package com.jimm0063.magi.document.utilities.core;

import java.util.List;

/**
 * A pluggable text utility. Implementations are Spring beans and are discovered automatically by
 * {@link ModuleRegistry}; they become available at {@code /api/v1/{id}/{action}}.
 */
public interface TextModule {

    /** URL-safe identifier, e.g. {@code markdown}. */
    String id();

    String description();

    List<String> actions();

    /** Path of this module's page in the web UI, or {@code null} if it has none. */
    default String webPath() {
        return null;
    }

    /**
     * Runs an action. Throw {@link ModuleException} for user-facing failures.
     */
    ModuleResponse execute(String action, ModuleRequest request);
}
