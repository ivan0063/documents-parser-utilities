package com.jimm0063.magi.document.utilities.modules.markdown;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.microsoft.playwright.Page;

import com.jimm0063.magi.document.utilities.engine.BrowserEngine;

/**
 * Renders Mermaid diagrams with the official mermaid.js (bundled, so it works offline) inside headless
 * Chromium, and screenshots each one to PNG.
 */
@Component
public class MermaidDiagramRenderer implements DiagramRenderer {

    static final Set<String> THEMES = Set.of("default", "neutral", "dark", "forest", "base");

    private static final String PAGE = """
            <!DOCTYPE html><html><head><meta charset="utf-8"><style>
            html, body { margin: 0; background: #ffffff; }
            .diagram { display: inline-block; padding: 12px; background: #ffffff; }
            </style></head><body></body></html>""";

    private static final String RENDER_ONE = """
            async ([code, index]) => {
              const id = 'mermaid-' + index;
              try {
                const { svg } = await mermaid.render(id, code);
                const host = document.createElement('div');
                host.id = 'host-' + index;
                host.className = 'diagram';
                host.innerHTML = svg;
                document.body.appendChild(host);
                const el = host.querySelector('svg');
                const maxWidth = parseFloat(el.style.maxWidth);
                if (maxWidth) {
                  el.style.width = maxWidth + 'px';
                  el.style.maxWidth = 'none';
                }
                return '';
              } catch (e) {
                document.getElementById('d' + id)?.remove();
                return String((e && e.message) || e || 'Unknown error');
              }
            }""";

    private final BrowserEngine engine;
    private final String mermaidJs;

    public MermaidDiagramRenderer(BrowserEngine engine) throws IOException {
        this.engine = engine;
        try (InputStream in = new ClassPathResource("mermaid/mermaid.min.js").getInputStream()) {
            this.mermaidJs = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Override
    public List<RenderedDiagram> render(List<String> sources, String theme) {
        if (sources.isEmpty()) {
            return List.of();
        }
        String safeTheme = THEMES.contains(theme) ? theme : "default";
        return engine.execute(context -> {
            Page page = context.newPage();
            page.setContent(PAGE);
            page.addScriptTag(new Page.AddScriptTagOptions().setContent(mermaidJs));
            page.evaluate("theme => mermaid.initialize({ startOnLoad: false, securityLevel: 'strict', "
                    + "suppressErrorRendering: true, theme })", safeTheme);

            List<RenderedDiagram> results = new ArrayList<>(sources.size());
            for (int i = 0; i < sources.size(); i++) {
                String source = sources.get(i);
                String error = (String) page.evaluate(RENDER_ONE, List.of(source, i));
                if (!error.isEmpty()) {
                    results.add(RenderedDiagram.failure(source, firstLines(error)));
                    continue;
                }
                byte[] png = page.locator("#host-" + i).screenshot();
                int displayWidth = (int) Math.round(pngWidth(png) / engine.deviceScaleFactor());
                results.add(RenderedDiagram.success(source, png, displayWidth));
            }
            return results;
        });
    }

    /** Width from the PNG IHDR header (bytes 16-19, big endian). */
    static int pngWidth(byte[] png) {
        return ((png[16] & 0xff) << 24) | ((png[17] & 0xff) << 16) | ((png[18] & 0xff) << 8) | (png[19] & 0xff);
    }

    private static String firstLines(String message) {
        String[] lines = message.strip().split("\\R");
        return String.join("\n", java.util.Arrays.copyOf(lines, Math.min(lines.length, 4)));
    }
}
