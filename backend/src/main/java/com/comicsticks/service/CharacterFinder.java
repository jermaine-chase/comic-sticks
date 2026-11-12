package com.comicsticks.service;

import com.comicsticks.dto.CharacterSheet;

/**
 * The character pass: reads a story and lists its main characters, each with a fixed design, and
 * its main places, each with a background. Runs before any panel is drawn, so every panel can draw
 * each character the same way.
 */
public interface CharacterFinder {

    CharacterSheet find(String storyText);

    /** How many LLM calls {@link #find} makes for this text, for the run time estimate. */
    int calls(String storyText);
}
