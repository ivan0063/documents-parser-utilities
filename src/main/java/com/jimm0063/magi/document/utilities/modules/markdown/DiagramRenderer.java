package com.jimm0063.magi.document.utilities.modules.markdown;

import java.util.List;

/**
 * Turns diagram sources into images. Returns one result per source, in the same order; a broken
 * diagram yields a failed result instead of an exception.
 */
public interface DiagramRenderer {

    List<RenderedDiagram> render(List<String> sources, String theme);
}
