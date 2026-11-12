package com.comicsticks.dto;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;

/** A hat that helps tell a character apart. Keep in sync with comic.models.ts. */
public enum Hat {
    @JsonEnumDefaultValue @JsonProperty("none") NONE,
    @JsonProperty("cap") CAP,
    @JsonProperty("top_hat") TOP_HAT,
    @JsonProperty("bowler") BOWLER,
    @JsonProperty("crown") CROWN,
    @JsonProperty("witch_hat") WITCH_HAT,
    @JsonProperty("beanie") BEANIE,
    @JsonProperty("straw_hat") STRAW_HAT
}
