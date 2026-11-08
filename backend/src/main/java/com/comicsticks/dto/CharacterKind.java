package com.comicsticks.dto;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Whether a character is drawn as a person or as an animal stick figure. Keep in sync with comic.models.ts. */
public enum CharacterKind {
    @JsonEnumDefaultValue @JsonProperty("person") PERSON,
    @JsonProperty("animal") ANIMAL
}
