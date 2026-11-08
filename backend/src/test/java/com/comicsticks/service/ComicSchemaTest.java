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

    private static Map<String, Object> itemProperties(Map<String, Object> panelProperties, String array) {
        return child(child(child(panelProperties, array), "items"), "properties");
    }

    @Test
    void asksForTheFeelingBeforeTheFaceAndTheBubbleTypeBeforeTheWords() {
        assertThat(itemProperties(panelProperties(), "characters").keySet())
                .containsExactly("name", "feeling", "expression", "pose", "facing");
        assertThat(itemProperties(panelProperties(), "dialogue").keySet()).containsExactly("speaker", "type", "text");
    }

    @Test
    void leavesFeelingsOutWhenTheyAreOff() {
        var panel = child(child(child(child(ComicSchema.story(1, false), "properties"), "panels"), "items"),
                "properties");

        var character = child(child(panel, "characters"), "items");
        assertThat(child(character, "properties").keySet()).doesNotContain("feeling");
        assertThat(character.get("required")).asList().doesNotContain("feeling");
    }

    @Test
    void offersEveryDialogueTypeIncludingWhisperAndThought() {
        assertThat(child(itemProperties(panelProperties(), "dialogue"), "type").get("enum")).asList()
                .containsExactly("speech", "thought", "shout", "whisper");
    }

    @Test
    void notesSchemaIsSmallAndStrict() {
        Map<String, Object> notes = ComicSchema.notes(3);

        assertThat(notes).containsEntry("additionalProperties", false);
        assertThat(child(child(notes, "properties"), "notes")).containsEntry("maxItems", 3).containsEntry("minItems", 0);
        assertThat(child(child(child(notes, "properties"), "notes"), "items").get("required")).asList()
                .containsExactly("phrase", "meaning");
    }
}
