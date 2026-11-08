package com.comicsticks.service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.comicsticks.dto.DialogueType;
import com.comicsticks.dto.Expression;
import com.comicsticks.dto.Facing;
import com.comicsticks.dto.Pose;
import com.comicsticks.dto.Scene;

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
        Map<String, Object> character = object(
                "name", string(),
                "pose", enumOf(Pose.values()),
                "expression", enumOf(Expression.values()),
                "facing", enumOf(Facing.values()));

        Map<String, Object> dialogue = object(
                "speaker", string(),
                "text", string(),
                "type", enumOf(DialogueType.values()));

        Map<String, Object> panel = object(
                "panelNumber", Map.of("type", "integer"),
                "scene", enumOf(Scene.values()),
                "caption", string(),
                "characters", arrayOf(character, 1, MAX_CHARACTERS_PER_PANEL),
                "dialogue", arrayOf(dialogue, 0, MAX_LINES_PER_PANEL));

        return object(
                "title", string(),
                "panels", arrayOf(panel, 1, maxPanels));
    }

    /** Builds an object schema from alternating property names and schemas, keeping their order. */
    private static Map<String, Object> object(Object... namesAndSchemas) {
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

    private static Map<String, Object> string() {
        return Map.of("type", "string");
    }

    private static Map<String, Object> arrayOf(Map<String, Object> items, int minItems, int maxItems) {
        return Map.of("type", "array", "items", items, "minItems", minItems, "maxItems", maxItems);
    }

    private static Map<String, Object> enumOf(Enum<?>[] constants) {
        return Map.of("type", "string", "enum", wireValues(constants));
    }

    /** The lowercase JSON spelling of each constant, matching the {@code @JsonProperty} values. */
    static List<String> wireValues(Enum<?>[] constants) {
        return Arrays.stream(constants).map(c -> c.name().toLowerCase()).toList();
    }
}
