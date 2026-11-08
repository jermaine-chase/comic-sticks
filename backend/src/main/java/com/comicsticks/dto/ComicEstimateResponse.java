package com.comicsticks.dto;

/**
 * What generating a comic would involve, returned before any LLM call is made.
 *
 * @param parts            number of LLM calls the story needs
 * @param panels           the most panels the comic will have
 * @param estimatedSeconds expected run time
 */
public record ComicEstimateResponse(int parts, int panels, int estimatedSeconds) {
}
