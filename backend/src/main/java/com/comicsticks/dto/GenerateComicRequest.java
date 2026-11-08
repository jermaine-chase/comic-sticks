package com.comicsticks.dto;

import jakarta.validation.constraints.NotBlank;

/** JSON body for {@code POST /api/comics/generate}. */
public record GenerateComicRequest(@NotBlank(message = "Story text is required.") String text) {
}
