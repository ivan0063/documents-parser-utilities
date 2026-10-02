package com.jimm0063.magi.document.utilities.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.jimm0063.magi.document.utilities.core.ModuleException;
import com.jimm0063.magi.document.utilities.core.ModuleResponse;

/**
 * Keeps the flat response contract (HTTP 200, {@code success: false}) for failures that happen outside
 * a module, e.g. an upload that is too large.
 */
@RestControllerAdvice(basePackages = "com.jimm0063.magi.document.utilities.api")
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ModuleException.class)
    public ModuleResponse handleModule(ModuleException e) {
        return ModuleResponse.error("core", "", e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ModuleResponse handleTooLarge(MaxUploadSizeExceededException e) {
        return ModuleResponse.error("core", "", "Upload is too large");
    }

    @ExceptionHandler(Exception.class)
    public ModuleResponse handleOther(Exception e) {
        log.error("Unhandled API error", e);
        return ModuleResponse.error("core", "", "Unexpected error: " + e.getMessage());
    }
}
