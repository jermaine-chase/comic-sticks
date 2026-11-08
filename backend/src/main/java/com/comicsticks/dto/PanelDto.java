package com.comicsticks.dto;

import java.util.List;

/**
 * One comic panel.
 *
 * @param panelNumber 1-based reading order
 * @param scene       backdrop
 * @param caption     narration box text; empty when the panel has none
 * @param characters  figures, left to right
 * @param dialogue    bubbles, in reading order
 * @param sourceText  the story's own words that this panel shows, exactly as written. Set by the
 *                    backend from the segment, never by the LLM. When a segment has several panels,
 *                    only the last carries the text and the others have an empty string.
 */
public record PanelDto(
        int panelNumber,
        Scene scene,
        String caption,
        List<CharacterDto> characters,
        List<DialogueDto> dialogue,
        String sourceText) {
}
