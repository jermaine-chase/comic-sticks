package com.comicsticks.service;

import com.comicsticks.dto.ComicEstimateResponse;
import com.comicsticks.dto.ComicStoryResponse;

/** Turns story text into panels. The result is cleaned up by {@link ComicStoryNormalizer}. */
public interface ComicGenerator {

    /** Predicts the work {@link #generate} would do, without doing any of it. */
    ComicEstimateResponse estimate(String storyText);

    ComicStoryResponse generate(String storyText);
}
