package com.jimm0063.magi.document.utilities.modules.markdown;

import org.springframework.stereotype.Component;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.Margin;

import com.jimm0063.magi.document.utilities.engine.BrowserEngine;

/**
 * Prints an HTML document to an A4 PDF with headless Chromium.
 */
@Component
public class PdfRenderer {

    private final BrowserEngine engine;

    public PdfRenderer(BrowserEngine engine) {
        this.engine = engine;
    }

    public byte[] toPdf(String html) {
        return engine.execute(context -> {
            Page page = context.newPage();
            page.setContent(html);
            return page.pdf(new Page.PdfOptions()
                    .setFormat("A4")
                    .setPrintBackground(true)
                    .setMargin(new Margin().setTop("18mm").setBottom("18mm").setLeft("16mm").setRight("16mm")));
        });
    }

    /** A safe file name derived from the document title. */
    public static String fileName(String title) {
        String base = title.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "").strip();
        if (base.length() > 80) {
            base = base.substring(0, 80).strip();
        }
        return (base.isEmpty() ? "document" : base) + ".pdf";
    }
}
