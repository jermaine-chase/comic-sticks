package com.comicsticks.dto;

import org.springframework.http.HttpStatus;

import com.comicsticks.exception.ComicGenerationException;

/**
 * How much picture support each passage gets. Level 1 is the most: a panel for every paragraph.
 * Levels 2 and 3 group more text under each panel, for readers who need fewer pictures.
 */
public enum SupportLevel {
    /** One panel per paragraph, with very long paragraphs cut into 2 to 3 lines each. */
    PARAGRAPH,
    /** One panel per group of 3 to 4 paragraphs. */
    GROUP,
    /** One panel per scene. */
    SCENE;

    public static final int DEFAULT = 1;

    public static SupportLevel of(Integer level) {
        int value = level == null ? DEFAULT : level;
        if (value < 1 || value > values().length) {
            throw new ComicGenerationException(HttpStatus.BAD_REQUEST, "The support level must be 1, 2 or 3.");
        }
        return values()[value - 1];
    }

    public int number() {
        return ordinal() + 1;
    }
}
