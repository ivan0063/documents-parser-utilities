package com.jimm0063.magi.document.utilities.modules.markdown;

import java.util.List;

/**
 * @param bodyHtml  HTML fragment styled by document.css (browser viewer and PDF)
 * @param notesHtml complete HTML document with inline styles only (Apple Notes / Shortcuts rich text)
 */
public record MarkdownResult(String title, String bodyHtml, String notesHtml, List<RenderedDiagram> diagrams) {

    public int diagramCount() {
        return diagrams.size();
    }

    public int failedDiagrams() {
        return (int) diagrams.stream().filter(d -> !d.ok()).count();
    }
}
