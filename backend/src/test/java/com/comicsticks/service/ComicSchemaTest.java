package com.comicsticks.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class ComicSchemaTest {

    @SuppressWarnings("unchecked")
    private static Map<String, Object> child(Map<String, Object> parent, String key) {
        return (Map<String, Object>) parent.get(key);
    }

    private static Map<String, Object> panelsArray() {
        return child(child(ComicSchema.story(12), "properties"), "panels");
    }

    private static Map<String, Object> panelProperties() {
        return child(child(panelsArray(), "items"), "properties");
    }

    @Test
    void keepsPropertiesInStoryTellingOrder() {
        assertThat(ComicSchema.story(12).get("required")).asList().containsExactly("title", "panels");
        assertThat(panelProperties().keySet())
                .containsExactly("panelNumber", "scene", "caption", "characters", "dialogue");
    }

    @Test
    void everyPanelMustHaveAFigure() {
        assertThat(child(panelProperties(), "characters")).containsEntry("minItems", 1);
    }

    @Test
    void capsPanelsAtTheConfiguredLimit() {
        assertThat(panelsArray()).containsEntry("maxItems", 12);
    }
}
