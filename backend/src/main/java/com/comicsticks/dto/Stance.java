package com.comicsticks.dto;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Whether a character stands up like a person or walks on four legs like a real animal. People are
 * always upright. Keep in sync with STANCES in comic.models.ts.
 */
public enum Stance {
    @JsonEnumDefaultValue @JsonProperty("upright") UPRIGHT,
    @JsonProperty("four_legs") FOUR_LEGS
}
