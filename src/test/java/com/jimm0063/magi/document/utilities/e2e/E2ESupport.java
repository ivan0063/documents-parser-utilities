package com.jimm0063.magi.document.utilities.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Base class for end-to-end tests: starts the real application on a random port (with the real headless
 * Chromium) and talks to it over HTTP exactly like Apple Shortcuts would.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class E2ESupport {

    protected static final JsonMapper JSON = JsonMapper.builder().build();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @Value("${local.server.port}")
    protected int port;

    protected String url(String path) {
        return "http://localhost:" + port + path;
    }

    protected HttpResponse<byte[]> get(String path, String... headers) {
        return send(request(path, headers).GET().build());
    }

    protected HttpResponse<byte[]> post(String path, String contentType, byte[] body, String... headers) {
        HttpRequest.Builder builder = request(path, headers).POST(HttpRequest.BodyPublishers.ofByteArray(body));
        if (contentType != null) {
            builder.header("Content-Type", contentType);
        }
        return send(builder.build());
    }

    protected HttpResponse<byte[]> postText(String path, String text, String... headers) {
        return post(path, "text/plain; charset=utf-8", text.getBytes(StandardCharsets.UTF_8), headers);
    }

    /** multipart/form-data with a single file part, like a Shortcut sending a "File" body field. */
    protected HttpResponse<byte[]> postFile(String path, String fieldName, String fileName, String content) {
        String boundary = "----magi" + UUID.randomUUID();
        String body = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"\r\n"
                + "Content-Type: text/markdown\r\n\r\n"
                + content + "\r\n"
                + "--" + boundary + "--\r\n";
        return post(path, "multipart/form-data; boundary=" + boundary, body.getBytes(StandardCharsets.UTF_8));
    }

    protected JsonNode json(HttpResponse<byte[]> response) {
        assertThat(response.statusCode()).as("HTTP status").isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
                type -> assertThat(type).startsWith("application/json"));
        return JSON.readTree(response.body());
    }

    protected static String text(HttpResponse<byte[]> response) {
        return new String(response.body(), StandardCharsets.UTF_8);
    }

    protected static String sample(String name) {
        try (InputStream in = E2ESupport.class.getResourceAsStream("/samples/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected static String toJson(Map<String, ?> body) {
        return JSON.writeValueAsString(body);
    }

    /**
     * The Shortcuts contract: one level of keys, values are scalars or lists of strings, and the base
     * fields are always present.
     */
    protected static void assertFlatContract(JsonNode body) {
        assertThat(body.isObject()).as("response is a JSON object").isTrue();
        for (String field : new String[] {"success", "module", "action", "result", "message"}) {
            assertThat(body.has(field)).as("base field '%s' present", field).isTrue();
        }
        assertThat(body.get("success").isBoolean()).isTrue();
        for (Map.Entry<String, JsonNode> entry : body.properties()) {
            JsonNode value = entry.getValue();
            if (value.isArray()) {
                for (JsonNode element : value) {
                    assertThat(element.isString()).as("'%s' only contains strings", entry.getKey()).isTrue();
                }
            } else {
                assertThat(value.isValueNode()).as("'%s' is a scalar", entry.getKey()).isTrue();
            }
        }
    }

    protected static boolean isPdf(byte[] bytes) {
        return bytes.length > 4 && new String(bytes, 0, 4, StandardCharsets.US_ASCII).equals("%PDF");
    }

    protected static boolean isPng(byte[] bytes) {
        return bytes.length > 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G';
    }

    private HttpRequest.Builder request(String path, String... headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url(path))).timeout(Duration.ofSeconds(90));
        if (headers.length > 0) {
            builder.headers(headers);
        }
        return builder;
    }

    private HttpResponse<byte[]> send(HttpRequest request) {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
