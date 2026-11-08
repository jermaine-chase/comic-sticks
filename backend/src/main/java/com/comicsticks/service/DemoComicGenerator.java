package com.comicsticks.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.comicsticks.config.ComicProperties;
import com.comicsticks.dto.CharacterDto;
import com.comicsticks.dto.ComicEstimateResponse;
import com.comicsticks.dto.ComicStoryResponse;
import com.comicsticks.dto.DialogueDto;
import com.comicsticks.dto.DialogueType;
import com.comicsticks.dto.Expression;
import com.comicsticks.dto.Facing;
import com.comicsticks.dto.PanelDto;
import com.comicsticks.dto.Pose;
import com.comicsticks.dto.Scene;

/**
 * Keyless stand-in for {@link ComicGenerationService}, active under the {@code demo} profile so
 * the UI can be tried offline. It is a crude rule-based splitter: one sentence per panel, with
 * quoted text as dialogue and the poses cycled. Real adaptation needs the LLM.
 */
@Service
@Profile("demo")
public class DemoComicGenerator implements ComicGenerator {

    private static final Pattern SENTENCE = Pattern.compile("[^.!?]+[.!?]+[\"”]?|[^.!?]+$");
    private static final Pattern QUOTE = Pattern.compile("[\"“]([^\"”]+)[\"”]");
    private static final Pattern NAME = Pattern.compile("\\b[A-Z][a-z]{2,}\\b");
    private static final List<String> NOT_NAMES = List.of("The", "Then", "When", "She", "They", "But",
            "And", "Once", "That", "There", "This", "His", "Her", "After", "Before", "Suddenly");

    private static final Pose[] POSES = Pose.values();
    private static final Expression[] EXPRESSIONS = Expression.values();
    private static final Scene[] SCENES = {Scene.OUTDOOR, Scene.INDOOR, Scene.NIGHT, Scene.PLAIN};

    private final int maxPanels;

    public DemoComicGenerator(ComicProperties properties) {
        this.maxPanels = properties.story().maxPanels();
    }

    @Override
    public ComicEstimateResponse estimate(String storyText) {
        return new ComicEstimateResponse(1, sentences(storyText).size(), 1);
    }

    @Override
    public ComicStoryResponse generate(String storyText) {
        List<String> sentences = sentences(storyText);
        List<String> cast = findCast(storyText);

        List<PanelDto> panels = new ArrayList<>();
        for (int i = 0; i < sentences.size(); i++) {
            panels.add(buildPanel(sentences.get(i), i, cast));
        }
        return new ComicStoryResponse("Demo Comic", panels);
    }

    private List<String> sentences(String storyText) {
        return SENTENCE.matcher(storyText).results()
                .map(m -> m.group().strip())
                .filter(s -> !s.isEmpty())
                .limit(maxPanels)
                .toList();
    }

    private static List<String> findCast(String text) {
        List<String> names = NAME.matcher(text).results()
                .map(m -> m.group())
                .filter(n -> !NOT_NAMES.contains(n))
                .distinct()
                .limit(2)
                .toList();
        return names.isEmpty() ? List.of("Hero") : names;
    }

    private static PanelDto buildPanel(String sentence, int index, List<String> cast) {
        List<CharacterDto> characters = new ArrayList<>();
        for (int c = 0; c < cast.size(); c++) {
            characters.add(new CharacterDto(
                    cast.get(c),
                    POSES[(index * cast.size() + c) % POSES.length],
                    EXPRESSIONS[(index + c) % EXPRESSIONS.length],
                    c == 0 ? Facing.RIGHT : Facing.LEFT));
        }

        List<DialogueDto> dialogue = new ArrayList<>();
        Matcher quote = QUOTE.matcher(sentence);
        while (quote.find()) {
            String line = quote.group(1).strip();
            DialogueType type = line.endsWith("!") ? DialogueType.SHOUT : DialogueType.SPEECH;
            dialogue.add(new DialogueDto(cast.get(dialogue.size() % cast.size()), line, type));
        }

        String caption = dialogue.isEmpty() ? sentence : "";
        return new PanelDto(index + 1, SCENES[index % SCENES.length], caption, characters, dialogue);
    }
}
