package com.comicsticks.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.comicsticks.dto.CharacterDto;
import com.comicsticks.dto.ComicOptions;
import com.comicsticks.dto.DialogueDto;
import com.comicsticks.dto.Expression;
import com.comicsticks.dto.NoteDto;
import com.comicsticks.dto.PanelDto;
import com.comicsticks.dto.ThingDto;

/**
 * Holds the LLM to the comic's content rules, which a JSON Schema cannot express:
 *
 * <ul>
 * <li>speech, shout and whisper bubbles must be words actually spoken in the text;
 * <li>a note's phrase must appear in the source text, and its meaning is kept short;
 * <li>a feeling is one plain word, and the face matches it;
 * <li>an object or animal is only drawn when the text names it.
 * </ul>
 */
@Component
public class PanelValidator {

    private static final int MAX_MEANING_SENTENCES = 2;
    private static final int MAX_NOTES = 3;
    static final int MAX_THINGS = 3;

    /** Describes each bubble that is not spoken in the text, for a retry prompt. Empty when all pass. */
    public List<String> dialogueProblems(List<PanelDto> panels, String text) {
        List<String> problems = new ArrayList<>();
        for (PanelDto panel : panels) {
            for (DialogueDto line : dialogue(panel)) {
                if (!isValid(line, text)) {
                    String type = line == null || line.type() == null ? "speech" : line.type().name().toLowerCase();
                    problems.add("The %s bubble \"%s\" is not words spoken in the text."
                            .formatted(type, line == null ? "" : line.text()));
                }
            }
        }
        return problems;
    }

    /** Removes bubbles that are not spoken in the text, and applies the feeling rules. */
    public List<PanelDto> clean(List<PanelDto> panels, String text, ComicOptions options) {
        return panels.stream()
                .filter(Objects::nonNull)
                .map(panel -> panel.withContent(
                        characters(panel).stream().filter(Objects::nonNull).map(c -> cleanFeeling(c, options)).toList(),
                        dialogue(panel).stream().filter(line -> isValid(line, text)).toList()))
                .toList();
    }

    /** Keeps notes whose phrase is in the source text, one per phrase, with short meanings. */
    public List<NoteDto> validNotes(List<NoteDto> notes, String sourceText) {
        if (notes == null) {
            return List.of();
        }
        List<NoteDto> kept = new ArrayList<>();
        for (NoteDto note : notes) {
            if (note == null || note.meaning() == null || note.meaning().isBlank()
                    || !TextMatch.contains(sourceText, note.phrase())
                    || kept.stream().anyMatch(k -> TextMatch.core(k.phrase()).equals(TextMatch.core(note.phrase())))) {
                continue;
            }
            kept.add(new NoteDto(note.phrase().strip(), shortened(note.meaning().strip())));
            if (kept.size() == MAX_NOTES) {
                break;
            }
        }
        return kept;
    }

    /**
     * Keeps things that {@code sourceText} names, once each, at most {@link #MAX_THINGS}. A holder
     * is kept only when it is one of {@code names} (in that spelling) and the thing can be held. A
     * thing whose word is a character's name, such as an animal character called "Dog", is that
     * character, so it is dropped.
     */
    public List<ThingDto> validThings(List<ThingDto> things, String sourceText, List<String> names) {
        if (things == null) {
            return List.of();
        }
        List<ThingDto> kept = new ArrayList<>();
        for (ThingDto candidate : things) {
            if (candidate == null || candidate.thing() == null
                    || !ThingWords.names(sourceText, candidate.thing())
                    || names.stream().anyMatch(name -> ThingWords.isNameOf(name, candidate.thing()))
                    || kept.stream().anyMatch(k -> k.thing() == candidate.thing())) {
                continue;
            }
            String holder = !candidate.thing().canBeHeld() || candidate.holder() == null ? ""
                    : names.stream().filter(candidate.holder().strip()::equalsIgnoreCase).findFirst().orElse("");
            kept.add(new ThingDto(candidate.thing(), holder));
            if (kept.size() == MAX_THINGS) {
                break;
            }
        }
        return kept;
    }

    static boolean isValid(DialogueDto line, String text) {
        if (line == null || line.text() == null || line.text().isBlank()) {
            return false;
        }
        boolean thought = line.type() != null && !line.type().isSpoken();
        return thought || TextMatch.isSpokenIn(text, line.text());
    }

    private static CharacterDto cleanFeeling(CharacterDto c, ComicOptions options) {
        String feeling = options.feelingLabels() ? FeelingExpressions.oneWord(c.feeling()) : "";
        // A known feeling decides the face, so the label and the face never disagree.
        Expression expression = FeelingExpressions.expressionFor(feeling).orElse(c.expression());
        return new CharacterDto(c.name(), c.pose(), expression, c.facing(), feeling);
    }

    private static String shortened(String meaning) {
        String[] sentences = meaning.split("(?<=[.!?])\\s+");
        return sentences.length <= MAX_MEANING_SENTENCES ? meaning
                : String.join(" ", List.of(sentences).subList(0, MAX_MEANING_SENTENCES));
    }

    private static List<DialogueDto> dialogue(PanelDto panel) {
        return panel.dialogue() == null ? List.of() : panel.dialogue();
    }

    private static List<CharacterDto> characters(PanelDto panel) {
        return panel.characters() == null ? List.of() : panel.characters();
    }
}
