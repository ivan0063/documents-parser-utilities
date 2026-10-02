package com.jimm0063.magi.document.utilities.modules.markdown;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PdfRendererTest {

    @Test
    void fileNameComesFromTitle() {
        assertThat(PdfRenderer.fileName("Payment Service Design")).isEqualTo("Payment Service Design.pdf");
    }

    @Test
    void fileNameRemovesUnsafeCharacters() {
        assertThat(PdfRenderer.fileName("a/b\\c:d*e?f\"g<h>i|j")).isEqualTo("abcdefghij.pdf");
    }

    @Test
    void fileNameFallsBackWhenEmpty() {
        assertThat(PdfRenderer.fileName("///")).isEqualTo("document.pdf");
    }

    @Test
    void pngWidthIsReadFromHeader() {
        byte[] header = new byte[24];
        header[18] = 0x02;
        header[19] = (byte) 0x80;
        assertThat(MermaidDiagramRenderer.pngWidth(header)).isEqualTo(640);
    }
}
