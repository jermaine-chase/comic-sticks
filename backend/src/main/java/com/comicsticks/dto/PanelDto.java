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
 */
public record PanelDto(
        int panelNumber,
        Scene scene,
        String caption,
        List<CharacterDto> characters,
        List<DialogueDto> dialogue) {
}
