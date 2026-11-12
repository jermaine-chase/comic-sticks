package com.comicsticks.dto;

/**
 * An object or background animal drawn in a panel.
 *
 * @param thing  what it is
 * @param holder the name of the character in the panel who holds or carries it, drawn in their hand;
 *               empty when it is on the ground or part of the scene
 */
public record ThingDto(Thing thing, String holder) {
}
