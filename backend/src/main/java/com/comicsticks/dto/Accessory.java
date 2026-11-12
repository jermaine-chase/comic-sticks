package com.comicsticks.dto;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Something a character always wears or carries. Keep in sync with comic.models.ts. */
public enum Accessory {
    @JsonEnumDefaultValue @JsonProperty("none") NONE,
    @JsonProperty("bow") BOW,
    @JsonProperty("glasses") GLASSES,
    @JsonProperty("beard") BEARD,
    @JsonProperty("cape") CAPE,
    @JsonProperty("scarf") SCARF,
    @JsonProperty("pocket_watch") POCKET_WATCH,
    @JsonProperty("bag") BAG,
    @JsonProperty("whiskers") WHISKERS
}
