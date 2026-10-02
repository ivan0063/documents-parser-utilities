package com.jimm0063.magi.document.utilities.modules.markdown;

import java.time.Instant;
import java.util.Base64;
import java.util.List;

import org.springframework.stereotype.Component;

import com.jimm0063.magi.document.utilities.config.MagiProperties;
import com.jimm0063.magi.document.utilities.core.ModuleException;
import com.jimm0063.magi.document.utilities.core.ModuleRequest;
import com.jimm0063.magi.document.utilities.core.ModuleResponse;
import com.jimm0063.magi.document.utilities.core.TextModule;
import com.jimm0063.magi.document.utilities.store.DocumentStore;
import com.jimm0063.magi.document.utilities.store.RenderedDocument;

/**
 * Markdown utilities. Main use case: an AI-generated technical document with mermaid diagrams becomes a
 * document with the diagrams rendered as images, ready for Apple Notes, the browser or a PDF.
 *
 * <ul>
 *   <li>{@code render}: result = Notes-friendly HTML; plus {@code url} (viewer page) and {@code pdfUrl}</li>
 *   <li>{@code diagrams}: items = each diagram as a base64 PNG (for "Base64 Decode" in Shortcuts)</li>
 * </ul>
 * Option {@code theme}: mermaid theme (default, neutral, dark, forest, base).
 */
@Component
public class MarkdownModule implements TextModule {

    public static final String ID = "markdown";

    private final MarkdownRenderer renderer;
    private final DocumentStore store;
    private final String defaultTheme;

    public MarkdownModule(MarkdownRenderer renderer, DocumentStore store, MagiProperties properties) {
        this.renderer = renderer;
        this.store = store;
        this.defaultTheme = properties.mermaid().theme();
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String description() {
        return "Render Markdown with mermaid diagrams as images (Apple Notes HTML, web page, PDF)";
    }

    @Override
    public List<String> actions() {
        return List.of("render", "diagrams");
    }

    @Override
    public String webPath() {
        return "/markdown";
    }

    @Override
    public ModuleResponse execute(String action, ModuleRequest request) {
        return switch (action) {
            case "render" -> render(request);
            case "diagrams" -> diagrams(request);
            default -> throw new ModuleException("Unknown action " + action);
        };
    }

    /** Renders and keeps the result in the in-memory store so it can be viewed or printed later. */
    public RenderedDocument renderAndStore(String markdown, String theme) {
        if (markdown == null || markdown.isBlank()) {
            throw new ModuleException("Text is empty");
        }
        MarkdownResult result = renderer.render(markdown, theme == null || theme.isBlank() ? defaultTheme : theme);
        RenderedDocument document = new RenderedDocument(store.newId(), result.title(), result.bodyHtml(),
                result.notesHtml(), result.diagramCount(), result.failedDiagrams(), Instant.now());
        store.put(document);
        return document;
    }

    private ModuleResponse render(ModuleRequest request) {
        RenderedDocument document = renderAndStore(request.requireText(), request.option("theme", defaultTheme));
        String viewUrl = request.baseUrl() + "/view/" + document.id();
        return ModuleResponse.ok(ID, "render", document.notesHtml())
                .with("title", document.title())
                .with("url", viewUrl)
                .with("pdfUrl", viewUrl + "/pdf")
                .with("notesUrl", viewUrl + "/notes")
                .with("diagramCount", document.diagramCount())
                .with("failedDiagrams", document.failedDiagrams())
                .message(diagramMessage(document.diagramCount(), document.failedDiagrams()));
    }

    private ModuleResponse diagrams(ModuleRequest request) {
        MarkdownResult result = renderer.render(request.requireText(), request.option("theme", defaultTheme));
        List<String> images = result.diagrams().stream()
                .filter(RenderedDiagram::ok)
                .map(d -> Base64.getEncoder().encodeToString(d.png()))
                .toList();
        return ModuleResponse.ok(ID, "diagrams", images.size() + " diagram(s) rendered")
                .items(images)
                .with("diagramCount", result.diagramCount())
                .with("failedDiagrams", result.failedDiagrams())
                .message(diagramMessage(result.diagramCount(), result.failedDiagrams()));
    }

    static String diagramMessage(int total, int failed) {
        return failed == 0 ? "OK" : failed + " of " + total + " diagram(s) could not be rendered";
    }
}
