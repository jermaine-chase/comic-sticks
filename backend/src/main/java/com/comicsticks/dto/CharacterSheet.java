package com.comicsticks.dto;

import java.util.List;

/** The main characters and places of a book, reviewed by the user before panels are drawn. */
public record CharacterSheet(List<CharacterDesign> characters, List<PlaceDto> places) {

    public static final CharacterSheet EMPTY = new CharacterSheet(List.of(), List.of());

    public boolean isEmpty() {
        return (characters == null || characters.isEmpty()) && (places == null || places.isEmpty());
    }
}
