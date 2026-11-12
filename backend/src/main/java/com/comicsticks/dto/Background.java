package com.comicsticks.dto;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;

/** A simple, recognizable backdrop for a place, reused whenever a scene is there. Keep in sync with comic.models.ts. */
public enum Background {
    @JsonEnumDefaultValue @JsonProperty("plain") PLAIN,
    @JsonProperty("room") ROOM,
    @JsonProperty("burrow") BURROW,
    @JsonProperty("cave") CAVE,
    @JsonProperty("forest") FOREST,
    @JsonProperty("field") FIELD,
    @JsonProperty("riverbank") RIVERBANK,
    @JsonProperty("garden") GARDEN,
    @JsonProperty("town") TOWN,
    @JsonProperty("castle") CASTLE,
    @JsonProperty("school") SCHOOL,
    @JsonProperty("beach") BEACH,
    @JsonProperty("boat") BOAT,
    @JsonProperty("kitchen") KITCHEN,
    @JsonProperty("bedroom") BEDROOM,
    @JsonProperty("shop") SHOP,
    @JsonProperty("library") LIBRARY,
    @JsonProperty("street") STREET,
    @JsonProperty("park") PARK,
    @JsonProperty("playground") PLAYGROUND,
    @JsonProperty("farm") FARM,
    @JsonProperty("mountain") MOUNTAIN,
    @JsonProperty("snow") SNOW,
    @JsonProperty("lake") LAKE,
    @JsonProperty("station") STATION,
    @JsonProperty("tunnel") TUNNEL
}
