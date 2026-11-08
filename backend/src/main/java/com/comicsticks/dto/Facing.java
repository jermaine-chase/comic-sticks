package com.comicsticks.dto;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;

/** The direction a figure looks. Poses are drawn facing right and mirrored for left. */
public enum Facing {
    @JsonEnumDefaultValue @JsonProperty("right") RIGHT,
    @JsonProperty("left") LEFT
}
