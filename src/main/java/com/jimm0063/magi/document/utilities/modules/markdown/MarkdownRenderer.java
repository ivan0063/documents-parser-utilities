package com.jimm0063.magi.document.utilities.modules.markdown;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.IndependentAttributeProviderFactory;
import com.vladsch.flexmark.html.AttributeProvider;
import com.vladsch.flexmark.html.renderer.LinkResolverContext;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.misc.Extension;

/**
 * Markdown to HTML with every mermaid code block replaced by its rendered image.
 *
 * <p>Diagrams are rendered once and embedded in two HTML variants: a class-based fragment for the
 * browser/PDF, and a complete inline-styled document for Apple Notes.
 */
@Component
public class MarkdownRenderer {

    private final DiagramRenderer diagramRenderer;
    private final MutableDataSet options = new MutableDataSet();
    private final Parser parser;

    public MarkdownRenderer(DiagramRenderer diagramRenderer) {
        this.diagramRenderer = diagramRenderer;
        options.set(Parser.EXTENSIONS, List.<Extension>of(
                TablesExtension.create(),
                TaskListExtension.create(),
                StrikethroughExtension.create(),
                AutolinkExtension.create()));
        this.parser = Parser.builder(options).build();
    }

    public MarkdownResult render(String markdown, String theme) {
        Document document = parser.parse(markdown);

        List<FencedCodeBlock> blocks = mermaidBlocks(document);
        List<RenderedDiagram> rendered = diagramRenderer.render(
                blocks.stream().map(b -> b.getContentChars().toString()).toList(), theme);
        Map<FencedCodeBlock, RenderedDiagram> diagrams = new IdentityHashMap<>();
        for (int i = 0; i < blocks.size(); i++) {
            diagrams.put(blocks.get(i), rendered.get(i));
        }

        String title = title(document);
        String bodyHtml = htmlRenderer(diagrams, false).render(document);
        String notesBody = htmlRenderer(diagrams, true).render(document);
        return new MarkdownResult(title, bodyHtml, DocumentHtml.notesDocument(title, notesBody), rendered);
    }

    /** Fenced code blocks whose info string starts with "mermaid" (e.g. ```mermaid or ```mermaid {theme}). */
    static List<FencedCodeBlock> mermaidBlocks(Document document) {
        List<FencedCodeBlock> blocks = new ArrayList<>();
        for (Node node : document.getDescendants()) {
            if (node instanceof FencedCodeBlock block) {
                String[] info = block.getInfo().toString().trim().split("[\\s{]+", 2);
                if (info[0].toLowerCase(Locale.ROOT).equals("mermaid")) {
                    blocks.add(block);
                }
            }
        }
        return blocks;
    }

    private static String title(Document document) {
        for (Node node : document.getDescendants()) {
            if (node instanceof Heading heading && heading.getLevel() == 1) {
                String text = heading.getText().toString().strip();
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }
        return "Document";
    }

    private HtmlRenderer htmlRenderer(Map<FencedCodeBlock, RenderedDiagram> diagrams, boolean inlineStyles) {
        HtmlRenderer.Builder builder = HtmlRenderer.builder(options)
                .nodeRendererFactory(dataHolder -> new MermaidNodeRenderer(diagrams, inlineStyles));
        if (inlineStyles) {
            builder.attributeProviderFactory(new IndependentAttributeProviderFactory() {
                @Override
                public AttributeProvider apply(LinkResolverContext context) {
                    return new NotesAttributeProvider();
                }
            });
        }
        return builder.build();
    }
}
