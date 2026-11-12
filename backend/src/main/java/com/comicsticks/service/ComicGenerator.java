package com.comicsticks.service;

import com.comicsticks.dto.CharacterSheet;
import com.comicsticks.dto.ComicEstimateResponse;
import com.comicsticks.dto.ComicOptions;
import com.comicsticks.dto.ComicStoryResponse;

/** Turns story text into panels. The result is cleaned up by {@link ComicStoryNormalizer}. */
public interface ComicGenerator {

    /**
     * Predicts the work {@link #generate} would do, without doing any of it.
     *
     * @param hasSheet whether a character sheet will be supplied; without one, generating runs the
     *                 character pass first
     */
    ComicEstimateResponse estimate(String storyText, ComicOptions options, boolean hasSheet);

    /**
     * @param sheet the reviewed character sheet to draw every character from. When it is empty, the
     *              character pass runs first. The result carries the sheet it was drawn from.
     */
    ComicStoryResponse generate(String storyText, ComicOptions options, CharacterSheet sheet);
}
