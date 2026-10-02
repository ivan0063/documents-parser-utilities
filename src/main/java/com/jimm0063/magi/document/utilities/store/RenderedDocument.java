package com.jimm0063.magi.document.utilities.store;

import java.time.Instant;

/**
 * A rendered Markdown document kept in memory for a short time.
 *
 * @param bodyHtml  styled HTML fragment for the browser viewer and the PDF
 * @param notesHtml complete, inline-styled HTML document for Apple Notes / Shortcuts
 */
public record RenderedDocument(String id, String title, String bodyHtml, String notesHtml,
                               int diagramCount, int failedDiagrams, Instant createdAt) {
}
