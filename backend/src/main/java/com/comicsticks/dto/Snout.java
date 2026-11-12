package com.comicsticks.dto;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;

/** An animal's snout or beak. Keep in sync with comic.models.ts. */
public enum Snout {
    @JsonEnumDefaultValue @JsonProperty("none") NONE,
    @JsonProperty("short") SHORT,
    @JsonProperty("long") LONG,
    @JsonProperty("beak") BEAK
}
