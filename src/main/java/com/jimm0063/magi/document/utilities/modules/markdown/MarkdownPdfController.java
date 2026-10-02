package com.jimm0063.magi.document.utilities.modules.markdown;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jimm0063.magi.document.utilities.api.RequestReader;
import com.jimm0063.magi.document.utilities.core.ModuleException;
import com.jimm0063.magi.document.utilities.core.ModuleRequest;
import com.jimm0063.magi.document.utilities.core.ModuleResponse;
import com.jimm0063.magi.document.utilities.store.RenderedDocument;

/**
 * {@code POST /api/v1/markdown/render?format=pdf}: returns the PDF file directly, so a Shortcut gets it
 * in one step. This is the only endpoint that does not answer with the JSON response (except on errors).
 */
@RestController
public class MarkdownPdfController {

    private final RequestReader requestReader;
    private final MarkdownModule module;
    private final PdfRenderer pdfRenderer;

    public MarkdownPdfController(RequestReader requestReader, MarkdownModule module, PdfRenderer pdfRenderer) {
        this.requestReader = requestReader;
        this.module = module;
        this.pdfRenderer = pdfRenderer;
    }

    @PostMapping(value = "/api/v1/markdown/render", params = "format=pdf")
    public ResponseEntity<?> renderPdf(HttpServletRequest request) throws IOException {
        try {
            ModuleRequest moduleRequest = requestReader.read(request);
            RenderedDocument document = module.renderAndStore(moduleRequest.requireText(),
                    moduleRequest.option("theme", null));
            return pdfResponse(document, pdfRenderer);
        } catch (ModuleException e) {
            return ResponseEntity.ok(ModuleResponse.error(MarkdownModule.ID, "render", e.getMessage()));
        }
    }

    public static ResponseEntity<byte[]> pdfResponse(RenderedDocument document, PdfRenderer pdfRenderer) {
        byte[] pdf = pdfRenderer.toPdf(DocumentHtml.printDocument(document.title(), document.bodyHtml()));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(PdfRenderer.fileName(document.title()), StandardCharsets.UTF_8)
                        .build().toString())
                .body(pdf);
    }
}
