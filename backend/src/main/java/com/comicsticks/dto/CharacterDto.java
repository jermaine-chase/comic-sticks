package com.comicsticks.dto;

/** A figure as it appears in one panel, listed left to right. */
public record CharacterDto(String name, Pose pose, Expression expression, Facing facing) {
}
