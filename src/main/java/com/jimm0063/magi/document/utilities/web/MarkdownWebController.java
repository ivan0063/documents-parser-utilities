package com.jimm0063.magi.document.utilities.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.jimm0063.magi.document.utilities.core.ModuleException;
import com.jimm0063.magi.document.utilities.modules.markdown.MarkdownModule;
import com.jimm0063.magi.document.utilities.modules.markdown.MarkdownPdfController;
import com.jimm0063.magi.document.utilities.modules.markdown.PdfRenderer;
import com.jimm0063.magi.document.utilities.store.DocumentStore;
import com.jimm0063.magi.document.utilities.store.RenderedDocument;

/**
 * Browser UI for the markdown module: a form at {@code /markdown} and the viewer at {@code /view/{id}}.
 * The viewer links are also returned by the API so a Shortcut can open them.
 */
@Controller
public class MarkdownWebController {

    private final MarkdownModule module;
    private final DocumentStore store;
    private final PdfRenderer pdfRenderer;

    public MarkdownWebController(MarkdownModule module, DocumentStore store, PdfRenderer pdfRenderer) {
        this.module = module;
        this.store = store;
        this.pdfRenderer = pdfRenderer;
    }

    @GetMapping("/markdown")
    public String form() {
        return "markdown";
    }

    @PostMapping("/markdown")
    public String render(@RequestParam(required = false) String text,
                         @RequestParam(required = false) MultipartFile file,
                         @RequestParam(required = false) String theme,
                         Model model) throws IOException {
        String markdown = file != null && !file.isEmpty()
                ? new String(file.getBytes(), StandardCharsets.UTF_8)
                : text;
        try {
            RenderedDocument document = module.renderAndStore(markdown, theme);
            return "redirect:/view/" + document.id();
        } catch (ModuleException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("text", text);
            return "markdown";
        }
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable String id, Model model, HttpServletResponse response) {
        RenderedDocument document = store.find(id).orElse(null);
        if (document == null) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return "expired";
        }
        model.addAttribute("doc", document);
        return "view";
    }

    @GetMapping("/view/{id}/pdf")
    public ResponseEntity<?> pdf(@PathVariable String id) {
        return store.find(id)
                .<ResponseEntity<?>>map(document -> MarkdownPdfController.pdfResponse(document, pdfRenderer))
                .orElseGet(() -> notFound());
    }

    @GetMapping("/view/{id}/notes")
    public ResponseEntity<String> notes(@PathVariable String id) {
        return store.find(id)
                .map(document -> ResponseEntity.ok()
                        .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
                        .body(document.notesHtml()))
                .orElseGet(() -> notFound());
    }

    private static <T> ResponseEntity<T> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}
