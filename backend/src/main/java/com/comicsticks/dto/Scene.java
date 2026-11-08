package com.comicsticks.dto;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;

/** The panel backdrop. Keep in sync with Scene in comic.models.ts. */
public enum Scene {
    @JsonEnumDefaultValue @JsonProperty("plain") PLAIN,
    @JsonProperty("indoor") INDOOR,
    @JsonProperty("outdoor") OUTDOOR,
    @JsonProperty("night") NIGHT
}
