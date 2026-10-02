package com.jimm0063.magi.document.utilities.modules.markdown;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;

/**
 * Builds complete HTML documents around rendered Markdown.
 */
public final class DocumentHtml {

    /** Shared stylesheet of the browser viewer and the PDF (also served at /css/document.css). */
    private static final String DOCUMENT_CSS = load("static/css/document.css");

    private DocumentHtml() {
    }

    /** Self-contained document with inline styles only, for "Make Rich Text from HTML" / Apple Notes. */
    public static String notesDocument(String title, String body) {
        return "<!DOCTYPE html>\n<html><head><meta charset=\"utf-8\"><title>" + Html.escape(title) + "</title></head>\n"
                + "<body style=\"font-family:-apple-system,Helvetica,Arial,sans-serif;font-size:15px;line-height:1.5;\">\n"
                + body + "</body></html>\n";
    }

    /** Self-contained, styled document used to print the PDF. */
    public static String printDocument(String title, String body) {
        return "<!DOCTYPE html>\n<html><head><meta charset=\"utf-8\"><title>" + Html.escape(title) + "</title>\n"
                + "<style>\n" + DOCUMENT_CSS + "\n</style></head>\n"
                + "<body class=\"print\"><article class=\"document\">\n" + body + "</article></body></html>\n";
    }

    private static String load(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Missing classpath resource " + path, e);
        }
    }
}
