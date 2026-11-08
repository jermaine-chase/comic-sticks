package com.comicsticks.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

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
import com.comicsticks.service.StorySegmenter.Part;

/**
 * Keyless stand-in for {@link ComicGenerationService}, active under the {@code demo} profile so
 * the UI can be tried offline. It splits the story with the same {@link StorySegmenter} and fills
 * each panel by simple rules: quoted text becomes dialogue and the poses are cycled. Real
 * adaptation needs the LLM.
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

    private final StorySegmenter segmenter;

    public DemoComicGenerator(StorySegmenter segmenter) {
        this.segmenter = segmenter;
    }

    @Override
    public ComicEstimateResponse estimate(String storyText) {
        List<Part> parts = segmenter.split(storyText);
        return new ComicEstimateResponse(parts.size(), parts.stream().mapToInt(Part::panels).sum(), 1);
    }

    @Override
    public ComicStoryResponse generate(String storyText) {
        List<String> cast = findCast(storyText);
        List<PanelDto> panels = new ArrayList<>();
        for (Part part : segmenter.split(storyText)) {
            List<String> groups = sentenceGroups(part.text(), part.panels());
            for (int g = 0; g < groups.size(); g++) {
                String source = g == groups.size() - 1 ? part.sourceText() : "";
                panels.add(buildPanel(groups.get(g), panels.size(), cast, source));
            }
        }
        return new ComicStoryResponse("Demo Comic", panels);
    }

    /** Shares a part's sentences out between its panels, in order. */
    private static List<String> sentenceGroups(String text, int panels) {
        List<String> sentences = SENTENCE.matcher(text).results()
                .map(m -> m.group().strip())
                .filter(s -> !s.isEmpty())
                .toList();
        if (sentences.isEmpty()) {
            return List.of(text);
        }
        int count = Math.min(panels, sentences.size());
        List<String> groups = new ArrayList<>();
        for (int g = 0; g < count; g++) {
            int from = g * sentences.size() / count;
            int to = (g + 1) * sentences.size() / count;
            groups.add(String.join(" ", sentences.subList(from, to)));
        }
        return groups;
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

    private static PanelDto buildPanel(String text, int index, List<String> cast, String sourceText) {
        List<CharacterDto> characters = new ArrayList<>();
        for (int c = 0; c < cast.size(); c++) {
            characters.add(new CharacterDto(
                    cast.get(c),
                    POSES[(index * cast.size() + c) % POSES.length],
                    EXPRESSIONS[(index + c) % EXPRESSIONS.length],
                    c == 0 ? Facing.RIGHT : Facing.LEFT));
        }

        List<DialogueDto> dialogue = new ArrayList<>();
        Matcher quote = QUOTE.matcher(text);
        while (quote.find()) {
            String line = quote.group(1).strip();
            DialogueType type = line.endsWith("!") ? DialogueType.SHOUT : DialogueType.SPEECH;
            dialogue.add(new DialogueDto(cast.get(dialogue.size() % cast.size()), line, type));
        }

        return new PanelDto(index + 1, SCENES[index % SCENES.length], "", characters, dialogue, sourceText);
    }
}
