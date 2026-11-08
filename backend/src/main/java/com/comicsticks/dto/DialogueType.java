package com.comicsticks.dto;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;

/** The bubble style for a line of dialogue. */
public enum DialogueType {
    @JsonEnumDefaultValue @JsonProperty("speech") SPEECH,
    @JsonProperty("thought") THOUGHT,
    @JsonProperty("shout") SHOUT
}
