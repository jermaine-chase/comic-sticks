package com.comicsticks.dto;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;

/** A hair style that helps tell a character apart. Keep in sync with comic.models.ts. */
public enum Hair {
    @JsonEnumDefaultValue @JsonProperty("none") NONE,
    @JsonProperty("short") SHORT,
    @JsonProperty("spiky") SPIKY,
    @JsonProperty("long") LONG,
    @JsonProperty("curly") CURLY,
    @JsonProperty("ponytail") PONYTAIL,
    @JsonProperty("bun") BUN
}
