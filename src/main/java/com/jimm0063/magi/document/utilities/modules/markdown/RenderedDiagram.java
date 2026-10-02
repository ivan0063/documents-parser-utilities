package com.jimm0063.magi.document.utilities.modules.markdown;

/**
 * Result of rendering one diagram: either a PNG or an error message.
 *
 * @param displayWidth width in CSS pixels (the PNG is rendered at a higher device scale factor for sharpness)
 */
public record RenderedDiagram(String source, byte[] png, int displayWidth, String error) {

    public static RenderedDiagram success(String source, byte[] png, int displayWidth) {
        return new RenderedDiagram(source, png, displayWidth, null);
    }

    public static RenderedDiagram failure(String source, String error) {
        return new RenderedDiagram(source, null, 0, error == null || error.isBlank() ? "Unknown error" : error);
    }

    public boolean ok() {
        return png != null;
    }
}
