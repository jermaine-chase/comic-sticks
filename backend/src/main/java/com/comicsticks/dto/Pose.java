package com.comicsticks.dto;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Every pose the frontend can draw. Keep in sync with POSES in comic.models.ts. */
public enum Pose {
    @JsonEnumDefaultValue @JsonProperty("standing") STANDING,
    @JsonProperty("walking") WALKING,
    @JsonProperty("running") RUNNING,
    @JsonProperty("waving") WAVING,
    @JsonProperty("shouting") SHOUTING,
    @JsonProperty("pointing") POINTING,
    @JsonProperty("thinking") THINKING,
    @JsonProperty("sitting") SITTING,
    @JsonProperty("jumping") JUMPING,
    @JsonProperty("cheering") CHEERING,
    @JsonProperty("fighting") FIGHTING,
    @JsonProperty("falling") FALLING
}
