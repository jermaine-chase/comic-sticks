package com.comicsticks.dto;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The bubble style for a line of dialogue. Keep in sync with DIALOGUE_TYPES in comic.models.ts.
 * Speech, shout and whisper are words actually spoken in the text, and the backend checks that they
 * are. A thought is what a character thinks, wants or plans, and may be put into words by the LLM.
 */
public enum DialogueType {
    @JsonEnumDefaultValue @JsonProperty("speech") SPEECH,
    @JsonProperty("thought") THOUGHT,
    @JsonProperty("shout") SHOUT,
    @JsonProperty("whisper") WHISPER;

    /** Whether the words must appear in the story text. */
    public boolean isSpoken() {
        return this != THOUGHT;
    }
}
