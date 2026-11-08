package com.comicsticks.dto;

import java.util.List;

/**
 * One comic panel.
 *
 * @param panelNumber 1-based reading order
 * @param scene       backdrop
 * @param caption     a scene-change box ("Later that night..."), shown at the top; empty when the time
 *                    and place have not changed
 * @param characters  figures, left to right
 * @param dialogue    bubbles, in reading order
 * @param sourceText  the story's own words that this panel shows, exactly as written. Set by the
 *                    backend from the segment, never by the LLM. When a segment has several panels,
 *                    only the last carries the text and the others have an empty string.
 * @param notes       "what it means" notes for phrases in {@code sourceText}; empty when there are none
 */
public record PanelDto(
        int panelNumber,
        Scene scene,
        String caption,
        List<CharacterDto> characters,
        List<DialogueDto> dialogue,
        String sourceText,
        List<NoteDto> notes) {

    public PanelDto withText(String sourceText, List<NoteDto> notes) {
        return new PanelDto(panelNumber, scene, caption, characters, dialogue, sourceText, notes);
    }

    public PanelDto withContent(List<CharacterDto> characters, List<DialogueDto> dialogue) {
        return new PanelDto(panelNumber, scene, caption, characters, dialogue, sourceText, notes);
    }
}
