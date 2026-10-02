package com.jimm0063.magi.document.utilities.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;

/**
 * End-to-end tests of the REST API used by Apple Shortcuts: real HTTP, real Mermaid rendering, real PDF.
 */
class ApiE2E extends E2ESupport {

    private static final String DOC = sample("technical-doc.md");

    @Test
    void listsModulesAndActions() {
        JsonNode body = json(get("/api/v1/modules"));

        assertFlatContract(body);
        assertThat(body.get("success").asBoolean()).isTrue();
        assertThat(strings(body.get("items"))).contains("markdown/render", "markdown/diagrams",
                "diagnostics/ping", "diagnostics/notes-test");
        assertThat(body.get("count").asInt()).isEqualTo(body.get("items").size());
    }

    @Test
    void listsActionsOfOneModule() {
        JsonNode body = json(get("/api/v1/markdown"));

        assertFlatContract(body);
        assertThat(strings(body.get("items"))).containsExactly("render", "diagrams");
    }

    @Test
    void rendersMarkdownWithDiagramsAsEmbeddedImages() {
        JsonNode body = json(postText("/api/v1/markdown/render", DOC));

        assertFlatContract(body);
        assertThat(body.get("success").asBoolean()).isTrue();
        assertThat(body.get("module").asString()).isEqualTo("markdown");
        assertThat(body.get("action").asString()).isEqualTo("render");
        assertThat(body.get("title").asString()).isEqualTo("Payment Service Design");
        assertThat(body.get("diagramCount").asInt()).isEqualTo(3);
        assertThat(body.get("failedDiagrams").asInt()).isEqualTo(1);
        assertThat(body.get("message").asString()).isEqualTo("1 of 3 diagram(s) could not be rendered");

        String html = body.get("result").asString();
        assertThat(html).startsWith("<!DOCTYPE html>").contains("<h1>Payment Service Design</h1>");
        assertThat(countOccurrences(html, "src=\"data:image/png;base64,")).isEqualTo(2);
        assertThat(html).contains("Diagram could not be rendered").contains("Parse error");
        assertThat(html).contains("regular code stays as code");
        assertThat(html).doesNotContain("sequenceDiagram");

        for (String image : embeddedImages(html)) {
            assertThat(isPng(Base64.getDecoder().decode(image))).as("embedded image is a PNG").isTrue();
        }
    }

    @Test
    void renderLinksServeViewerNotesAndPdf() {
        JsonNode body = json(postText("/api/v1/markdown/render", DOC));
        String base = url("");

        String viewUrl = body.get("url").asString();
        assertThat(viewUrl).startsWith(base + "/view/");
        assertThat(body.get("pdfUrl").asString()).isEqualTo(viewUrl + "/pdf");
        assertThat(body.get("notesUrl").asString()).isEqualTo(viewUrl + "/notes");

        HttpResponse<byte[]> viewer = get(viewUrl.substring(base.length()));
        assertThat(viewer.statusCode()).isEqualTo(200);
        assertThat(text(viewer)).contains("Payment Service Design").contains("class=\"mermaid-diagram\"")
                .contains("Copy for Apple Notes");

        HttpResponse<byte[]> notes = get(viewUrl.substring(base.length()) + "/notes");
        assertThat(notes.statusCode()).isEqualTo(200);
        assertThat(notes.headers().firstValue("Content-Type").orElse("")).startsWith("text/html");
        assertThat(text(notes)).isEqualTo(body.get("result").asString());

        HttpResponse<byte[]> pdf = get(viewUrl.substring(base.length()) + "/pdf");
        assertThat(pdf.statusCode()).isEqualTo(200);
        assertThat(pdf.headers().firstValue("Content-Type")).hasValue("application/pdf");
        assertThat(pdf.headers().firstValue("Content-Disposition").orElse(""))
                .contains("attachment").contains("Payment Service Design.pdf");
        assertThat(isPdf(pdf.body())).isTrue();
    }

    @Test
    void rawModeReturnsOnlyTheHtml() {
        HttpResponse<byte[]> response = postText("/api/v1/markdown/render?raw=true", DOC);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).startsWith("text/plain");
        assertThat(text(response)).startsWith("<!DOCTYPE html>").contains("data:image/png;base64,");
    }

    @Test
    void pdfFormatReturnsTheFileDirectly() {
        HttpResponse<byte[]> response = postText("/api/v1/markdown/render?format=pdf", DOC);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).hasValue("application/pdf");
        assertThat(isPdf(response.body())).isTrue();
        assertThat(response.body().length).isGreaterThan(10_000);
    }

    @Test
    void pdfFormatWithEmptyTextReturnsJsonError() {
        JsonNode body = json(postText("/api/v1/markdown/render?format=pdf", ""));

        assertFlatContract(body);
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("message").asString()).isEqualTo("Text is empty");
    }

    @Test
    void acceptsJsonBodyWithOptions() {
        String request = toJson(Map.of("text", "# From JSON\n\n```mermaid\nflowchart TD\n  A --> B\n```\n",
                "theme", "forest"));
        JsonNode body = json(post("/api/v1/markdown/render", "application/json",
                request.getBytes(StandardCharsets.UTF_8)));

        assertFlatContract(body);
        assertThat(body.get("success").asBoolean()).isTrue();
        assertThat(body.get("title").asString()).isEqualTo("From JSON");
        assertThat(body.get("diagramCount").asInt()).isEqualTo(1);
        assertThat(body.get("failedDiagrams").asInt()).isZero();
        assertThat(body.get("message").asString()).isEqualTo("OK");
    }

    @Test
    void acceptsAFileUpload() {
        JsonNode body = json(postFile("/api/v1/markdown/render", "file", "doc.md", DOC));

        assertFlatContract(body);
        assertThat(body.get("success").asBoolean()).isTrue();
        assertThat(body.get("diagramCount").asInt()).isEqualTo(3);
    }

    @Test
    void acceptsARawFileBodyWithAnyContentType() {
        JsonNode body = json(post("/api/v1/markdown/render", "application/octet-stream",
                DOC.getBytes(StandardCharsets.UTF_8)));

        assertThat(body.get("success").asBoolean()).isTrue();
        assertThat(body.get("title").asString()).isEqualTo("Payment Service Design");
    }

    @Test
    void diagramsActionReturnsBase64PngItems() {
        JsonNode body = json(postText("/api/v1/markdown/diagrams", DOC));

        assertFlatContract(body);
        assertThat(body.get("success").asBoolean()).isTrue();
        assertThat(body.get("count").asInt()).isEqualTo(2);
        assertThat(body.get("diagramCount").asInt()).isEqualTo(3);
        assertThat(body.get("failedDiagrams").asInt()).isEqualTo(1);
        for (String image : strings(body.get("items"))) {
            assertThat(isPng(Base64.getDecoder().decode(image))).isTrue();
        }
    }

    @Test
    void emptyTextIsAFlatError() {
        JsonNode body = json(postText("/api/v1/markdown/render", "   "));

        assertFlatContract(body);
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("result").asString()).isEmpty();
        assertThat(body.get("message").asString()).isEqualTo("Text is empty");
    }

    @Test
    void invalidJsonIsAFlatError() {
        JsonNode body = json(post("/api/v1/markdown/render", "application/json",
                "{not json".getBytes(StandardCharsets.UTF_8)));

        assertFlatContract(body);
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("message").asString()).contains("JSON");
    }

    @Test
    void unknownModuleAndActionAreFlatErrors() {
        JsonNode unknownModule = json(postText("/api/v1/nope/render", "x"));
        assertFlatContract(unknownModule);
        assertThat(unknownModule.get("success").asBoolean()).isFalse();
        assertThat(unknownModule.get("message").asString()).contains("Unknown module");

        JsonNode unknownAction = json(postText("/api/v1/markdown/nope", "x"));
        assertFlatContract(unknownAction);
        assertThat(unknownAction.get("success").asBoolean()).isFalse();
        assertThat(unknownAction.get("message").asString()).contains("Unknown action").contains("render");
    }

    @Test
    void rawModeOnErrorReturnsReadableText() {
        HttpResponse<byte[]> response = postText("/api/v1/markdown/render?raw=true", "");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(text(response)).isEqualTo("ERROR: Text is empty");
    }

    @Test
    void notesTestReturnsHtmlWithAnEmbeddedImage() {
        JsonNode body = json(get("/api/v1/diagnostics/notes-test"));

        assertFlatContract(body);
        List<String> images = embeddedImages(body.get("result").asString());
        assertThat(images).hasSize(1);
        assertThat(isPng(Base64.getDecoder().decode(images.get(0)))).isTrue();
    }

    @Test
    void pingEchoesText() {
        assertThat(json(get("/api/v1/diagnostics/ping")).get("result").asString()).isEqualTo("pong");
        assertThat(json(postText("/api/v1/diagnostics/ping", "hola ñ")).get("result").asString())
                .isEqualTo("hola ñ");
    }

    @Test
    void expiredOrUnknownDocumentsAreNotFound() {
        assertThat(get("/view/does-not-exist").statusCode()).isEqualTo(404);
        assertThat(get("/view/does-not-exist/pdf").statusCode()).isEqualTo(404);
        assertThat(get("/view/does-not-exist/notes").statusCode()).isEqualTo(404);
    }

    private static List<String> strings(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(node -> values.add(node.asString()));
        return values;
    }

    private static List<String> embeddedImages(String html) {
        List<String> images = new ArrayList<>();
        String marker = "data:image/png;base64,";
        int index = html.indexOf(marker);
        while (index >= 0) {
            int start = index + marker.length();
            int end = html.indexOf('"', start);
            images.add(html.substring(start, end));
            index = html.indexOf(marker, end);
        }
        return images;
    }

    private static int countOccurrences(String text, String token) {
        int count = 0;
        for (int i = text.indexOf(token); i >= 0; i = text.indexOf(token, i + token.length())) {
            count++;
        }
        return count;
    }
}
