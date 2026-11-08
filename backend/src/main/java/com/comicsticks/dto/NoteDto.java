package com.comicsticks.dto;

/**
 * A "what it means" note for a phrase a literal reader could misunderstand, such as an idiom.
 *
 * @param phrase  the words exactly as they appear in the panel's source text
 * @param meaning one or two short, literal sentences saying what the phrase really means
 */
public record NoteDto(String phrase, String meaning) {
}
