package com.jimm0063.magi.document.utilities.modules.markdown;

import com.vladsch.flexmark.ast.BlockQuote;
import com.vladsch.flexmark.ast.Code;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.ast.IndentedCodeBlock;
import com.vladsch.flexmark.ext.tables.TableBlock;
import com.vladsch.flexmark.ext.tables.TableCell;
import com.vladsch.flexmark.html.AttributeProvider;
import com.vladsch.flexmark.html.renderer.AttributablePart;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.html.MutableAttributes;

/**
 * Adds inline styles for the Apple Notes variant. Shortcuts' "Make Rich Text from HTML" ignores
 * stylesheets, so anything that matters (monospace code, table borders) must be inline.
 */
class NotesAttributeProvider implements AttributeProvider {

    private static final String MONO = "font-family:Menlo,Monaco,monospace;";

    @Override
    public void setAttributes(Node node, AttributablePart part, MutableAttributes attributes) {
        if (part != AttributablePart.NODE) {
            return;
        }
        String style = switch (node) {
            case FencedCodeBlock ignored -> MONO + "font-size:12px;background:#f6f8fa;padding:8px;white-space:pre-wrap;";
            case IndentedCodeBlock ignored -> MONO + "font-size:12px;background:#f6f8fa;padding:8px;white-space:pre-wrap;";
            case Code ignored -> MONO + "background:#f0f0f0;";
            case BlockQuote ignored -> "border-left:3px solid #cccccc;margin-left:0;padding-left:10px;color:#555555;";
            case TableBlock ignored -> "border-collapse:collapse;";
            case TableCell ignored -> "border:1px solid #cccccc;padding:4px 8px;";
            case Image ignored -> "max-width:100%;height:auto;";
            default -> null;
        };
        if (style != null) {
            attributes.addValue("style", style);
        }
    }
}
