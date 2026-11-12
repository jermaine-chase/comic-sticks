package com.comicsticks.dto;

import java.util.List;

/**
 * The full comic returned by {@code POST /api/comics/generate}.
 *
 * @param characters the character sheet the panels were drawn from, including any character added
 *                   because a panel named someone not on it
 * @param places     the places the panels' {@code place} names refer to
 * @param bookends   the chapter's opening "Who's who" and closing "What happened"; null until added
 */
public record ComicStoryResponse(String title, List<PanelDto> panels, List<CharacterDesign> characters,
        List<PlaceDto> places, ChapterBookends bookends) {

    public ComicStoryResponse(String title, List<PanelDto> panels) {
        this(title, panels, List.of(), List.of(), null);
    }

    public ComicStoryResponse(String title, List<PanelDto> panels, List<CharacterDesign> characters,
            List<PlaceDto> places) {
        this(title, panels, characters, places, null);
    }

    public ComicStoryResponse withBookends(ChapterBookends bookends) {
        return new ComicStoryResponse(title, panels, characters, places, bookends);
    }
}
