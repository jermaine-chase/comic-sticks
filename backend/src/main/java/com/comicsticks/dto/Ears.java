package com.comicsticks.dto;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Animal ears. Keep in sync with comic.models.ts. */
public enum Ears {
    @JsonEnumDefaultValue @JsonProperty("none") NONE,
    @JsonProperty("round") ROUND,
    @JsonProperty("pointy") POINTY,
    @JsonProperty("long") LONG,
    @JsonProperty("floppy") FLOPPY
}
