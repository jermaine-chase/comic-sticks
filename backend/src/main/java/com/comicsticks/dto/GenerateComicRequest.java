package com.comicsticks.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * JSON body for {@code POST /api/comics/estimate} and {@code /generate}.
 *
 * @param text         the story
 * @param supportLevel 1, 2 or 3; 1 when absent
 */
public record GenerateComicRequest(
        @NotBlank(message = "Story text is required.") String text,
        Integer supportLevel) {

    public ComicOptions options() {
        return ComicOptions.of(supportLevel);
    }
}
