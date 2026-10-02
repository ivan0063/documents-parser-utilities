package com.jimm0063.magi.document.utilities.core;

/**
 * An expected failure whose message is safe to show to the user (e.g. "Text is empty").
 */
public class ModuleException extends RuntimeException {

    public ModuleException(String message) {
        super(message);
    }

    public ModuleException(String message, Throwable cause) {
        super(message, cause);
    }
}
