package com.comicsticks.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Request and response bodies for {@code /api/saved-comics}. */
public final class SavedComicDtos {

    private SavedComicDtos() {
    }

    /**
     * The options a comic was drawn with, as the frontend sends them. Saved so a reopened comic can
     * be shown, and later redrawn, the same way.
     */
    public record SavedOptions(Integer supportLevel, Boolean feelingLabels, Boolean notes, Boolean questions,
            Boolean things) {
    }

    /**
     * Saves a comic, or replaces a saved one.
     *
     * @param comic   the comic as the reader last edited it
     * @param options the options it was drawn with; may be absent
     */
    public record SaveComicRequest(@NotNull(message = "A comic is required.") @Valid ComicStoryResponse comic,
            SavedOptions options) {
    }

    /** One line in the list of saved comics. */
    public record SavedComicSummary(UUID id, String title, int panelCount, Instant createdAt, Instant updatedAt) {
    }

    /** A saved comic with everything needed to show it again. */
    public record SavedComicResponse(UUID id, String title, int panelCount, Instant createdAt, Instant updatedAt,
            ComicStoryResponse comic, SavedOptions options) {
    }
}
