package com.comicsticks.dto;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;

/** An animal's tail. Keep in sync with comic.models.ts. */
public enum AnimalTail {
    @JsonEnumDefaultValue @JsonProperty("none") NONE,
    @JsonProperty("long") LONG,
    @JsonProperty("bushy") BUSHY,
    @JsonProperty("curly") CURLY,
    @JsonProperty("short") SHORT
}
