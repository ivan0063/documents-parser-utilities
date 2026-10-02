package com.jimm0063.magi.document.utilities.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ModuleResponseTest {

    @Test
    void okHasAllBaseFieldsInOrder() {
        ModuleResponse response = ModuleResponse.ok("markdown", "render", "<p>hi</p>");

        assertThat(response.asMap()).containsExactly(
                Map.entry("success", true),
                Map.entry("module", "markdown"),
                Map.entry("action", "render"),
                Map.entry("result", "<p>hi</p>"),
                Map.entry("message", "OK"));
    }

    @Test
    void errorHasEmptyResultAndMessage() {
        ModuleResponse response = ModuleResponse.error("markdown", "render", "Text is empty");

        assertThat(response.success()).isFalse();
        assertThat(response.result()).isEmpty();
        assertThat(response.get("message")).isEqualTo("Text is empty");
    }

    @Test
    void nullsBecomeEmptyStrings() {
        ModuleResponse response = ModuleResponse.ok(null, null, null);

        assertThat(response.asMap().values()).doesNotContainNull();
    }

    @Test
    void itemsAddsListAndCount() {
        ModuleResponse response = ModuleResponse.ok("m", "a", "").items(List.of("x", "y"));

        assertThat(response.get("items")).isEqualTo(List.of("x", "y"));
        assertThat(response.get("count")).isEqualTo(2);
    }

    @Test
    void acceptsScalarExtraFields() {
        ModuleResponse response = ModuleResponse.ok("m", "a", "")
                .with("url", "http://x")
                .with("diagramCount", 3)
                .with("ready", true);

        assertThat(response.asMap()).containsEntry("url", "http://x").containsEntry("diagramCount", 3)
                .containsEntry("ready", true);
    }

    @Test
    void rejectsNestedValuesToKeepTheResponseFlat() {
        ModuleResponse response = ModuleResponse.ok("m", "a", "");

        assertThatThrownBy(() -> response.with("nested", Map.of("a", "b")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> response.with("objects", List.of(Map.of("a", "b"))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> response.with("numbers", List.of(1, 2)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void baseFieldsCannotBeOverwrittenWithWith() {
        assertThatThrownBy(() -> ModuleResponse.ok("m", "a", "").with("success", false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mapIsReadOnly() {
        assertThatThrownBy(() -> ModuleResponse.ok("m", "a", "").asMap().put("x", "y"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
