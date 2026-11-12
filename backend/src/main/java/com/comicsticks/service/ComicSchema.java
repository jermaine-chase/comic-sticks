package com.comicsticks.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.comicsticks.dto.DialogueType;
import com.comicsticks.dto.Expression;
import com.comicsticks.dto.Facing;
import com.comicsticks.dto.Pose;
import com.comicsticks.dto.Scene;
import com.comicsticks.dto.Thing;

/**
 * The JSON Schema sent to the LLM as its required response format. It mirrors the DTO records,
 * and takes its enum values from the Java enums, so the two cannot disagree on allowed values.
 * Follows the rules of strict structured outputs: every property is required and no object
 * allows additional properties.
 *
 * <p>Property order matters. Ollama generates keys in schema order, so the model decides who is
 * in a panel before writing what they say. Keep the order stable and in story-telling sequence.
 */
final class ComicSchema {

    private static final int MAX_CHARACTERS_PER_PANEL = 4;
    private static final int MAX_LINES_PER_PANEL = 3;

    private ComicSchema() {
    }

    static Map<String, Object> story(int maxPanels) {
        return story(maxPanels, true, List.of());
    }

    /**
     * @param feelings whether each character carries a feeling word. It comes before the face, so
     *                 the model names the feeling first and then draws it.
     * @param places   the names of the places on the character sheet. When there are any, each panel
     *                 names its place from this list, or "" for somewhere else.
     */
    static Map<String, Object> story(int maxPanels, boolean feelings, List<String> places) {
        List<Object> characterFields = new ArrayList<>(List.of("name", string()));
        if (feelings) {
            characterFields.addAll(List.of("feeling", string()));
        }
        characterFields.addAll(List.of(
                "expression", enumOf(Expression.values()),
                "pose", enumOf(Pose.values()),
                "facing", enumOf(Facing.values())));
        Map<String, Object> character = object(characterFields.toArray());

        // The type comes before the words, so the model decides first whether a line was spoken.
        Map<String, Object> dialogue = object(
                "speaker", string(),
                "type", enumOf(DialogueType.values()),
                "text", string());

        List<Object> panelFields = new ArrayList<>(List.of("panelNumber", Map.of("type", "integer")));
        if (!places.isEmpty()) {
            List<String> choices = new ArrayList<>(places);
            choices.add("");
            panelFields.addAll(List.of("place", Map.of("type", "string", "enum", choices)));
        }
        panelFields.addAll(List.of(
                "scene", enumOf(Scene.values()),
                "caption", string(),
                "characters", arrayOf(character, 1, MAX_CHARACTERS_PER_PANEL),
                "dialogue", arrayOf(dialogue, 0, MAX_LINES_PER_PANEL)));
        Map<String, Object> panel = object(panelFields.toArray());

        return object(
                "title", string(),
                "panels", arrayOf(panel, 1, maxPanels));
    }

    /** The reply for the separate, small "what it means" call. */
    static Map<String, Object> notes(int maxNotes) {
        Map<String, Object> note = object(
                "phrase", string(),
                "meaning", string());
        return object("notes", arrayOf(note, 0, maxNotes));
    }

    /**
     * The reply for the separate, small call that picks the objects and background animals to draw.
     *
     * @param holders the names of the characters in the passage's panels; "" means nobody holds it
     */
    static Map<String, Object> things(int maxThings, List<String> holders) {
        List<String> choices = new ArrayList<>(holders);
        choices.add("");
        Map<String, Object> thing = object(
                "thing", enumOf(Thing.values()),
                "holder", Map.of("type", "string", "enum", choices));
        return object("things", arrayOf(thing, 0, maxThings));
    }

    /** Builds an object schema from alternating property names and schemas, keeping their order. */
    static Map<String, Object> object(Object... namesAndSchemas) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (int i = 0; i < namesAndSchemas.length; i += 2) {
            properties.put((String) namesAndSchemas[i], namesAndSchemas[i + 1]);
        }
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.copyOf(properties.keySet()));
        schema.put("additionalProperties", false);
        return schema;
    }

    static Map<String, Object> string() {
        return Map.of("type", "string");
    }

    static Map<String, Object> arrayOf(Map<String, Object> items, int minItems, int maxItems) {
        return Map.of("type", "array", "items", items, "minItems", minItems, "maxItems", maxItems);
    }

    static Map<String, Object> enumOf(Enum<?>[] constants) {
        return Map.of("type", "string", "enum", wireValues(constants));
    }

    /** The lowercase JSON spelling of each constant, matching the {@code @JsonProperty} values. */
    static List<String> wireValues(Enum<?>[] constants) {
        return Arrays.stream(constants).map(c -> c.name().toLowerCase()).toList();
    }
}
