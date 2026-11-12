package com.comicsticks.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * JSON body for {@code POST /api/comics/estimate}, {@code /characters} and {@code /generate}.
 *
 * @param text           the story
 * @param supportLevel   1, 2 or 3; 1 when absent
 * @param feelingLabels  feeling tags on or off; on when absent
 * @param notes          "what it means" notes on or off; on when absent
 * @param questions      comprehension questions at the end of the chapter on or off; off when absent
 * @param things         objects and background animals in panels on or off; on when absent
 * @param characterSheet the reviewed sheet to draw from; when absent, the character pass runs first
 */
public record GenerateComicRequest(
        @NotBlank(message = "Story text is required.") String text,
        Integer supportLevel,
        Boolean feelingLabels,
        Boolean notes,
        Boolean questions,
        Boolean things,
        CharacterSheet characterSheet) {

    public ComicOptions options() {
        return ComicOptions.of(supportLevel, feelingLabels, notes, questions, things);
    }
}
