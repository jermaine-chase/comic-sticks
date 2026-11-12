package com.comicsticks.dto;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Every face the frontend can draw. Keep in sync with EXPRESSIONS in comic.models.ts. */
public enum Expression {
    @JsonEnumDefaultValue @JsonProperty("neutral") NEUTRAL,
    @JsonProperty("happy") HAPPY,
    @JsonProperty("sad") SAD,
    @JsonProperty("angry") ANGRY,
    @JsonProperty("surprised") SURPRISED,
    @JsonProperty("scared") SCARED,
    @JsonProperty("confused") CONFUSED,
    @JsonProperty("worried") WORRIED,
    @JsonProperty("excited") EXCITED,
    @JsonProperty("proud") PROUD,
    @JsonProperty("embarrassed") EMBARRASSED,
    @JsonProperty("determined") DETERMINED,
    @JsonProperty("tired") TIRED
}
