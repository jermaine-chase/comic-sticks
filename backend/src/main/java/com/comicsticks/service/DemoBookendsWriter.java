package com.comicsticks.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.comicsticks.dto.ChapterBookends;
import com.comicsticks.dto.ChapterBookends.Question;
import com.comicsticks.dto.ComicOptions;
import com.comicsticks.dto.ComicStoryResponse;
import com.comicsticks.dto.DialogueDto;
import com.comicsticks.dto.DialogueType;
import com.comicsticks.dto.PanelDto;

/**
 * The demo profile's bookends, by simple rules: the recap is the first sentence of evenly spaced
 * panels, the main character is whoever appears most, and the questions ask who said the first
 * line of speech and which picture shows the end.
 */
@Service
@Profile("demo")
public class DemoBookendsWriter implements BookendsWriter {

    @Override
    public ChapterBookends write(ComicStoryResponse story, ComicOptions options) {
        String main = BookendsRules.mainCharacter(story, null);
        String feeling = BookendsRules.endingFeeling(story, main, null);
        List<Question> questions = new ArrayList<>();
        if (options.questions()) {
            story.panels().stream()
                    .flatMap(p -> p.dialogue().stream())
                    .filter(d -> d.type() != DialogueType.THOUGHT)
                    .findFirst()
                    .map(line -> BookendsRules.whoQuestion(story, "Who said \"%s\"?".formatted(trimmed(line)),
                            line.speaker()))
                    .ifPresent(questions::add);
            List<PanelDto> text = BookendsRules.textPanels(story);
            if (!text.isEmpty()) {
                questions.add(BookendsRules.pictureQuestion(story, "Which picture shows the end of the chapter?",
                        text.getLast().panelNumber()));
            }
        }
        return new ChapterBookends(
                BookendsRules.whosWho(story, null),
                BookendsRules.recap(story, BookendsRules.fallbackRecap(story, BookendsRules.MIN_RECAP), main, feeling),
                BookendsRules.limit(questions));
    }

    private static String trimmed(DialogueDto line) {
        return line.text().replaceAll("[\\s,;:]+$", "");
    }
}
