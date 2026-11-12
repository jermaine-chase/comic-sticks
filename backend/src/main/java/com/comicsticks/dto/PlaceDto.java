package com.comicsticks.dto;

/**
 * A main location, drawn with the same background every time a scene is there.
 *
 * @param name        the name panels use for it, such as "River bank"
 * @param description one plain line
 * @param background  the backdrop preset
 */
public record PlaceDto(String name, String description, Background background) {
}
