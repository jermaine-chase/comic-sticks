package com.comicsticks.dto;

/**
 * A figure as it appears in one panel, listed left to right.
 *
 * @param feeling one plain word for what the character feels, such as "scared", shown as a small tag
 *                near the figure; empty when there is none or feeling labels are off
 */
public record CharacterDto(String name, Pose pose, Expression expression, Facing facing, String feeling) {
}
