package com.comicsticks.dto;

import java.util.List;

/**
 * The full comic returned by {@code POST /api/comics/generate}.
 *
 * @param characters the character sheet the panels were drawn from, including any character added
 *                   because a panel named someone not on it
 * @param places     the places the panels' {@code place} names refer to
 */
public record ComicStoryResponse(String title, List<PanelDto> panels, List<CharacterDesign> characters,
        List<PlaceDto> places) {

    public ComicStoryResponse(String title, List<PanelDto> panels) {
        this(title, panels, List.of(), List.of());
    }
}
