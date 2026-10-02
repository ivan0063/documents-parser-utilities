package com.jimm0063.magi.document.utilities.modules.markdown;

import java.util.Base64;
import java.util.Map;
import java.util.Set;

import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;

/**
 * Replaces mermaid code blocks with the rendered image (base64 PNG), or with a visible warning plus the
 * original code when the diagram could not be rendered. Other code blocks render normally.
 */
class MermaidNodeRenderer implements NodeRenderer {

    private static final String MONO = "font-family:Menlo,Monaco,monospace;font-size:12px;";

    private final Map<FencedCodeBlock, RenderedDiagram> diagrams;
    private final boolean inlineStyles;

    MermaidNodeRenderer(Map<FencedCodeBlock, RenderedDiagram> diagrams, boolean inlineStyles) {
        this.diagrams = diagrams;
        this.inlineStyles = inlineStyles;
    }

    @Override
    public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
        return Set.of(new NodeRenderingHandler<>(FencedCodeBlock.class, this::render));
    }

    private void render(FencedCodeBlock node, NodeRendererContext context, HtmlWriter html) {
        RenderedDiagram diagram = diagrams.get(node);
        if (diagram == null) {
            context.delegateRender();
            return;
        }
        html.line();
        if (diagram.ok()) {
            String src = "data:image/png;base64," + Base64.getEncoder().encodeToString(diagram.png());
            html.raw("<p class=\"diagram\"" + style("text-align:center;") + ">"
                    + "<img class=\"mermaid-diagram\" src=\"" + src + "\" alt=\"Diagram\" width=\""
                    + diagram.displayWidth() + "\"" + style("max-width:100%;height:auto;") + "></p>");
        } else {
            html.raw("<div class=\"diagram-error\""
                    + style("border:1px solid #e0a800;background:#fff8e1;padding:8px 12px;margin:12px 0;") + ">"
                    + "<p" + style("margin:0 0 6px 0;color:#8a6d00;") + ">"
                    + "<strong>&#9888; Diagram could not be rendered</strong></p>"
                    + "<pre class=\"diagram-error-message\""
                    + style(MONO + "white-space:pre-wrap;margin:0 0 6px 0;color:#8a6d00;") + ">"
                    + Html.escape(diagram.error()) + "</pre>"
                    + "<pre" + style(MONO + "background:#f6f8fa;padding:8px;white-space:pre-wrap;margin:0;") + "><code>"
                    + Html.escape(diagram.source()) + "</code></pre></div>");
        }
        html.line();
    }

    private String style(String css) {
        return inlineStyles ? " style=\"" + css + "\"" : "";
    }
}
