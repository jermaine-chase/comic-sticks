package com.comicsticks.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.comicsticks.config.ComicProperties;
import com.comicsticks.dto.CharacterDto;
import com.comicsticks.dto.ComicStoryResponse;
import com.comicsticks.dto.DialogueDto;
import com.comicsticks.dto.DialogueType;
import com.comicsticks.dto.Expression;
import com.comicsticks.dto.Facing;
import com.comicsticks.dto.NoteDto;
import com.comicsticks.dto.PanelDto;
import com.comicsticks.dto.Pose;
import com.comicsticks.dto.Scene;
import com.comicsticks.exception.ComicGenerationException;

/**
 * Enforces what a JSON Schema cannot, so the frontend can rely on these guarantees: nothing is
 * null, panels are numbered 1..n, and every line of dialogue has a figure in its panel to point
 * at. Lines whose speaker is missing from the panel gain a figure, unless the "speaker" is a
 * narrator, in which case the line is dropped: the narration is already under the panel.
 */
@Component
public class ComicStoryNormalizer {

    private static final String DEFAULT_TITLE = "Untitled Comic";

    private final int maxPanels;

    public ComicStoryNormalizer(ComicProperties properties) {
        this.maxPanels = properties.story().maxPanels();
    }

    public ComicStoryResponse normalize(ComicStoryResponse story) {
        List<PanelDto> source = story == null || story.panels() == null ? List.of() : story.panels();
        List<PanelDto> panels = new ArrayList<>();
        for (PanelDto panel : source.stream().filter(Objects::nonNull).limit(maxPanels).toList()) {
            panels.add(normalizePanel(panel, panels.size() + 1));
        }
        if (panels.isEmpty()) {
            throw new ComicGenerationException(HttpStatus.BAD_GATEWAY,
                    "No comic panels came back for that story. Please try again.");
        }
        return new ComicStoryResponse(textOr(story.title(), DEFAULT_TITLE), panels);
    }

    private static PanelDto normalizePanel(PanelDto panel, int number) {
        List<CharacterDto> characters = new ArrayList<>(listOf(panel.characters()).stream()
                .filter(c -> c != null && !text(c.name()).isEmpty())
                .map(ComicStoryNormalizer::normalizeCharacter)
                .toList());

        List<DialogueDto> dialogue = new ArrayList<>();
        for (DialogueDto line : listOf(panel.dialogue())) {
            if (line == null || text(line.text()).isEmpty()) {
                continue;
            }
            String speaker = text(line.speaker());
            // Narration is already under the panel, word for word, so a narrator bubble adds nothing.
            if (speaker.isEmpty() || speaker.equalsIgnoreCase("narrator")) {
                continue;
            }
            String matched = matchCharacter(characters, speaker);
            dialogue.add(new DialogueDto(matched, text(line.text()),
                    line.type() == null ? DialogueType.SPEECH : line.type()));
        }

        List<NoteDto> notes = listOf(panel.notes()).stream()
                .filter(n -> n != null && !text(n.phrase()).isEmpty() && !text(n.meaning()).isEmpty())
                .map(n -> new NoteDto(text(n.phrase()), text(n.meaning())))
                .toList();

        return new PanelDto(number, panel.scene() == null ? Scene.PLAIN : panel.scene(),
                text(panel.caption()), characters, dialogue, panel.sourceText() == null ? "" : panel.sourceText(),
                notes);
    }

    private static CharacterDto normalizeCharacter(CharacterDto c) {
        return new CharacterDto(
                text(c.name()),
                c.pose() == null ? Pose.STANDING : c.pose(),
                c.expression() == null ? Expression.NEUTRAL : c.expression(),
                c.facing() == null ? Facing.RIGHT : c.facing(),
                text(c.feeling()));
    }

    /** Returns the panel's spelling of the speaker's name, adding a figure if there is none. */
    private static String matchCharacter(List<CharacterDto> characters, String speaker) {
        return characters.stream()
                .map(CharacterDto::name)
                .filter(speaker::equalsIgnoreCase)
                .findFirst()
                .orElseGet(() -> {
                    characters.add(new CharacterDto(speaker, Pose.STANDING, Expression.NEUTRAL, Facing.LEFT, ""));
                    return speaker;
                });
    }

    private static <T> List<T> listOf(List<T> list) {
        return list == null ? List.of() : list;
    }

    private static String text(String value) {
        return value == null ? "" : value.strip();
    }

    private static String textOr(String value, String fallback) {
        String stripped = text(value);
        return stripped.isEmpty() ? fallback : stripped;
    }
}
