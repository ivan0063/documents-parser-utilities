package com.jimm0063.magi.document.utilities.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import tools.jackson.databind.JsonNode;

/**
 * With {@code magi.api-key} set, the API requires the key; the web UI stays open.
 */
@TestPropertySource(properties = "magi.api-key=test-secret")
class ApiKeyE2E extends E2ESupport {

    @Test
    void rejectsRequestsWithoutTheKey() {
        JsonNode body = json(get("/api/v1/modules"));

        assertFlatContract(body);
        assertThat(body.get("success").asBoolean()).isFalse();
        assertThat(body.get("message").asString()).contains("API key");
    }

    @Test
    void rejectsAWrongKey() {
        assertThat(json(get("/api/v1/modules", "X-API-Key", "wrong")).get("success").asBoolean()).isFalse();
    }

    @Test
    void acceptsTheKeyInTheHeader() {
        assertThat(json(get("/api/v1/modules", "X-API-Key", "test-secret")).get("success").asBoolean()).isTrue();
    }

    @Test
    void webUiDoesNotNeedTheKey() {
        assertThat(get("/").statusCode()).isEqualTo(200);
    }
}
