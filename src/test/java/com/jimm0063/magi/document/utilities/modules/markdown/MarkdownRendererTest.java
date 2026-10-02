package com.jimm0063.magi.document.utilities.modules.markdown;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Markdown → HTML without a browser: diagrams come from a stub renderer.
 */
class MarkdownRendererTest {

    /** A 1x1 PNG header is enough for the HTML; the content is irrelevant here. */
    private static final byte[] FAKE_PNG = {(byte) 0x89, 'P', 'N', 'G'};

    private final List<String> receivedSources = new ArrayList<>();
    private final List<String> receivedThemes = new ArrayList<>();

    private final MarkdownRenderer renderer = new MarkdownRenderer((sources, theme) -> {
        receivedSources.addAll(sources);
        receivedThemes.add(theme);
        return sources.stream()
                .map(s -> s.contains("BROKEN")
                        ? RenderedDiagram.failure(s, "Parse error on line 2")
                        : RenderedDiagram.success(s, FAKE_PNG, 320))
                .toList();
    });

    @Test
    void replacesMermaidBlocksWithEmbeddedImages() {
        MarkdownResult result = renderer.render("""
                # Title

                ```mermaid
                flowchart LR
                  A --> B
                ```
                """, "default");

        assertThat(receivedSources).containsExactly("flowchart LR\n  A --> B\n");
        assertThat(result.diagramCount()).isEqualTo(1);
        assertThat(result.failedDiagrams()).isZero();
        assertThat(result.bodyHtml())
                .contains("<img class=\"mermaid-diagram\" src=\"data:image/png;base64,")
                .contains("width=\"320\"")
                .doesNotContain("flowchart LR");
        assertThat(result.notesHtml()).contains("data:image/png;base64,");
    }

    @Test
    void keepsOtherCodeBlocksAsCode() {
        MarkdownResult result = renderer.render("""
                ```java
                int x = 1 < 2 ? 1 : 0;
                ```
                """, "default");

        assertThat(receivedSources).isEmpty();
        assertThat(result.bodyHtml()).contains("<pre><code class=\"language-java\">int x = 1 &lt; 2 ? 1 : 0;");
    }

    @Test
    void recognisesMermaidInfoStringVariants() {
        renderer.render("""
                ```Mermaid
                graph TD; A-->B
                ```

                ```mermaid {theme: dark}
                graph TD; C-->D
                ```

                ```mermaidjs
                not a diagram
                ```
                """, "default");

        assertThat(receivedSources).containsExactly("graph TD; A-->B\n", "graph TD; C-->D\n");
    }

    @Test
    void brokenDiagramShowsWarningAndEscapedSource() {
        MarkdownResult result = renderer.render("""
                ```mermaid
                BROKEN <script>alert(1)</script>
                ```
                """, "default");

        assertThat(result.failedDiagrams()).isEqualTo(1);
        assertThat(result.bodyHtml())
                .contains("class=\"diagram-error\"")
                .contains("Diagram could not be rendered")
                .contains("Parse error on line 2")
                .contains("BROKEN &lt;script&gt;alert(1)&lt;/script&gt;")
                .doesNotContain("<script>");
    }

    @Test
    void notesVariantUsesInlineStylesOnly() {
        MarkdownResult result = renderer.render("""
                # Notes

                Some `code` here.

                ```mermaid
                flowchart LR; A-->B
                ```

                | a | b |
                |---|---|
                | 1 | 2 |
                """, "default");

        assertThat(result.notesHtml())
                .startsWith("<!DOCTYPE html>")
                .doesNotContain("<style")
                .doesNotContain("<link")
                .contains("<code style=\"font-family:Menlo")
                .contains("style=\"max-width:100%;height:auto;\"")
                .contains("border:1px solid #cccccc");
        assertThat(result.bodyHtml()).doesNotContain("style=");
    }

    @Test
    void titleIsFirstLevelOneHeading() {
        assertThat(renderer.render("intro\n\n## Sub\n\n# Real Title\n", "default").title()).isEqualTo("Real Title");
        assertThat(renderer.render("no headings", "default").title()).isEqualTo("Document");
    }

    @Test
    void rendersGfmExtensions() {
        String html = renderer.render("""
                - [x] done
                - [ ] todo

                ~~old~~ https://example.com
                """, "default").bodyHtml();

        assertThat(html).contains("task-list-item").contains("<del>old</del>")
                .contains("<a href=\"https://example.com\">");
    }

    @Test
    void passesThemeToDiagramRenderer() {
        renderer.render("```mermaid\ngraph TD; A-->B\n```\n", "forest");

        assertThat(receivedThemes).containsExactly("forest");
    }
}
