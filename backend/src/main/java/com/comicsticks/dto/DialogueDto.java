package com.comicsticks.dto;

/** One bubble. {@code speaker} matches the name of a character in the same panel. */
public record DialogueDto(String speaker, String text, DialogueType type) {
}
