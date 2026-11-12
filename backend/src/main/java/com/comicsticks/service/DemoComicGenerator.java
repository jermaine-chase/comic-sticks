package com.comicsticks.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.comicsticks.dto.CharacterDesign;
import com.comicsticks.dto.CharacterDto;
import com.comicsticks.dto.CharacterSheet;
import com.comicsticks.dto.ComicEstimateResponse;
import com.comicsticks.dto.ComicOptions;
import com.comicsticks.dto.ComicStoryResponse;
import com.comicsticks.dto.DialogueDto;
import com.comicsticks.dto.DialogueType;
import com.comicsticks.dto.Expression;
import com.comicsticks.dto.Facing;
import com.comicsticks.dto.NoteDto;
import com.comicsticks.dto.PanelDto;
import com.comicsticks.dto.PlaceDto;
import com.comicsticks.dto.Pose;
import com.comicsticks.dto.Scene;
import com.comicsticks.dto.Thing;
import com.comicsticks.dto.ThingDto;
import com.comicsticks.service.StorySegmenter.Part;

/**
 * Keyless stand-in for {@link ComicGenerationService}, active under the {@code demo} profile so
 * the UI can be tried offline. It splits the story with the same {@link StorySegmenter} and fills
 * every field by simple rules:
 *
 * <ul>
 * <li>quoted text becomes a bubble for the character named nearby, a shout if it ends in "!" or is
 * "shouted", and a whisper if it is "whispered";
 * <li>a feeling word in the text, such as "afraid", becomes the feeling of the character it is
 * about, and sets the face;
 * <li>an opening phrase such as "Later that night" or "Back at the river" becomes the caption;
 * <li>a short list of common idioms becomes "what it means" notes;
 * <li>an object or animal the text names is drawn, in the hand of a character who holds, carries,
 * takes or finds it in the same sentence.
 * </ul>
 *
 * Real adaptation needs the LLM.
 */
@Service
@Profile("demo")
public class DemoComicGenerator implements ComicGenerator {

    private static final Pattern SENTENCE = Pattern.compile("[^.!?]+[.!?]+[\"”']?|[^.!?]+$");
    private static final Pattern QUOTE = Pattern.compile("[\"“]([^\"”]+)[\"”]");
    private static final Pattern WHISPERED = Pattern.compile("(?i)\\bwhisper(ed|s|ing)?\\b");
    private static final Pattern SHOUTED = Pattern.compile("(?i)\\b(shout|yell|scream|cr(y|ie))(ed|s|ing)?\\b");
    private static final Pattern SCENE_CHANGE = Pattern.compile(
            "^(?:Later(?: that \\w+)?|Meanwhile|Soon|Hours later|Days later|Years later|The next (?:day|morning|night)"
                    + "|That (?:night|evening|morning|afternoon)|Back (?:at|in|on|home)(?: the \\w+)?)\\b");
    private static final Pattern HOLDS = Pattern.compile(
            "(?i)\\b(?:held|holds|holding|carried|carries|carrying|picked up|picks up|took|takes|grabbed|grabs"
                    + "|found|finds|lifted|lifts)\\b");
    private static final Pattern NIGHT = Pattern.compile("(?i)\\b(night|dark|moon|stars|midnight|evening)\\b");
    private static final Pattern INDOOR = Pattern.compile(
            "(?i)\\b(room|house|home|kitchen|attic|hall|burrow|cave|inside|bed|castle|school)\\b");

    /** Idioms the demo can explain, keyed by a pattern that finds them. */
    private static final Map<Pattern, String> IDIOMS = new LinkedHashMap<>();

    static {
        idiom("(?:\\w+['’]s|his|her|my|their|our) heart sank",
                "This means they suddenly felt very disappointed or worried. Their heart did not really move.");
        idiom("raining cats and dogs", "This means it was raining very hard. No animals fell from the sky.");
        idiom("butterflies in (?:his|her|my|their) (?:stomach|tummy)",
                "This means they felt nervous. There were no real butterflies.");
        idiom("over the moon", "This means very happy. Nobody went near the moon.");
        idiom("piece of cake", "This means something was very easy. It has nothing to do with real cake.");
        idiom("cold feet", "This means they felt too nervous to do something. Their feet were not really cold.");
        idiom("lost (?:his|her|my|their) temper", "This means they suddenly became very angry.");
        idiom("under the weather", "This means feeling a little ill.");
        idiom("in a flash", "This means very quickly.");
        idiom("couldn't believe (?:his|her|my|their) eyes",
                "This means they were very surprised by what they saw. Their eyes were working fine.");
        idiom("jumped out of (?:his|her|my|their) skin",
                "This means they got a big fright. Nobody really left their skin.");
        idiom("held (?:his|her|my|their) breath",
                "This means they stopped breathing for a moment because they were nervous or waiting.");
    }

    private static void idiom(String regex, String meaning) {
        IDIOMS.put(Pattern.compile("(?i)\\b" + regex + "\\b"), meaning);
    }

    private static final Pose[] POSES = {Pose.STANDING, Pose.WALKING, Pose.POINTING, Pose.WAVING, Pose.THINKING,
            Pose.SITTING};

    private static final int MAX_CAST_PER_PANEL = 3;

    private final StorySegmenter segmenter;
    private final CharacterFinder finder;
    private final SheetRules sheetRules;

    public DemoComicGenerator(StorySegmenter segmenter, CharacterFinder finder, SheetRules sheetRules) {
        this.segmenter = segmenter;
        this.finder = finder;
        this.sheetRules = sheetRules;
    }

    @Override
    public ComicEstimateResponse estimate(String storyText, ComicOptions options, boolean hasSheet) {
        List<Part> parts = segmenter.split(storyText, options.supportLevel());
        return new ComicEstimateResponse(parts.size(), parts.stream().mapToInt(Part::panels).sum(), 1);
    }

    @Override
    public ComicStoryResponse generate(String storyText, ComicOptions options, CharacterSheet suppliedSheet) {
        CharacterSheet sheet = sheetRules.normalize(suppliedSheet == null || suppliedSheet.isEmpty()
                ? finder.find(storyText) : suppliedSheet);
        List<String> sheetNames = sheet.characters().stream().map(CharacterDesign::name).toList();
        List<String> cast = sheetNames.isEmpty() ? List.of("Hero") : sheetNames.subList(0, 1);
        String place = "";
        List<PanelDto> panels = new ArrayList<>();
        for (Part part : segmenter.split(storyText, options.supportLevel())) {
            List<String> groups = sentenceGroups(part.text(), part.panels());
            for (int g = 0; g < groups.size(); g++) {
                String text = groups.get(g);
                cast = castOf(text, sheetNames, cast);
                place = placeOf(text, sheet.places(), place);
                boolean last = g == groups.size() - 1;
                List<NoteDto> notes = last && options.notes() ? notes(part.sourceText()) : List.of();
                PanelDto panel = buildPanel(text, panels.size(), cast, options);
                List<ThingDto> things = options.things()
                        ? PanelValidator.withoutCharacterAnimals(things(text, cast), cast.stream()
                                .flatMap(name -> SheetRules.find(sheet.characters(), name).stream())
                                .map(CharacterDesign::species)
                                .toList())
                        : List.of();
                panels.add(new PanelDto(panel.panelNumber(), panel.scene(), panel.caption(), panel.characters(),
                        panel.dialogue(), last ? part.sourceText() : "", notes, place, things));
            }
        }
        return sheetRules.reconcile(new ComicStoryResponse("Demo Comic", panels), sheet);
    }

    /** The sheet characters named in the text, in order of mention; otherwise the previous panel's. */
    private static List<String> castOf(String text, List<String> sheetNames, List<String> previous) {
        List<String> named = sheetNames.stream()
                .filter(name -> Pattern.compile("\\b" + Pattern.quote(name) + "\\b").matcher(text).find())
                .sorted((a, b) -> text.indexOf(a) - text.indexOf(b))
                .limit(MAX_CAST_PER_PANEL)
                .toList();
        return named.isEmpty() ? previous : named;
    }

    /** The first sheet place whose words appear in the text; otherwise the previous panel's place. */
    private static String placeOf(String text, List<PlaceDto> places, String previous) {
        return DemoCharacterFinder.placesIn(text).stream()
                .map(PlaceDto::name)
                .filter(name -> places.stream().anyMatch(p -> p.name().equals(name)))
                .findFirst()
                .orElse(previous);
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

    private static PanelDto buildPanel(String text, int index, List<String> cast, ComicOptions options) {
        List<DialogueDto> dialogue = dialogue(text, cast);
        String feeling = options.feelingLabels() ? feeling(text) : "";
        String feelingOwner = feeling.isEmpty() ? "" : nearestName(text, cast, text.toLowerCase(Locale.ROOT)
                .indexOf(feeling));

        List<CharacterDto> characters = new ArrayList<>();
        for (int c = 0; c < cast.size(); c++) {
            String name = cast.get(c);
            boolean feels = name.equals(feelingOwner);
            Expression expression = feels
                    ? FeelingExpressions.expressionFor(feeling).orElse(Expression.NEUTRAL) : Expression.NEUTRAL;
            boolean speaks = dialogue.stream().anyMatch(d -> d.speaker().equals(name));
            Pose pose = speaks && dialogue.stream().anyMatch(d -> d.type() == DialogueType.SHOUT) ? Pose.SHOUTING
                    : POSES[(index + c) % POSES.length];
            characters.add(new CharacterDto(name, pose, expression, c == 0 ? Facing.RIGHT : Facing.LEFT,
                    feels ? feeling : ""));
        }
        return new PanelDto(index + 1, scene(text), caption(text), characters, dialogue, "", List.of());
    }

    private static List<DialogueDto> dialogue(String text, List<String> cast) {
        List<DialogueDto> dialogue = new ArrayList<>();
        Matcher quote = QUOTE.matcher(text);
        while (quote.find() && dialogue.size() < 3) {
            String line = quote.group(1).strip();
            String after = text.substring(quote.end(), Math.min(text.length(), quote.end() + 30));
            DialogueType type = WHISPERED.matcher(after).find() ? DialogueType.WHISPER
                    : line.endsWith("!") || SHOUTED.matcher(after).find() ? DialogueType.SHOUT
                    : DialogueType.SPEECH;
            String speaker = nearestName(text, cast, quote.end());
            if (speaker.isEmpty()) {
                speaker = cast.get(dialogue.size() % cast.size());
            }
            dialogue.add(new DialogueDto(speaker, line, type));
        }
        return dialogue;
    }

    /** The cast member named closest to {@code position}, preferring a name just after it. */
    private static String nearestName(String text, List<String> cast, int position) {
        String best = "";
        int bestDistance = Integer.MAX_VALUE;
        for (String name : cast) {
            Matcher m = Pattern.compile("\\b" + Pattern.quote(name) + "\\b").matcher(text);
            while (m.find()) {
                int distance = m.start() >= position ? m.start() - position : (position - m.end()) + 5;
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = name;
                }
            }
        }
        return best;
    }

    private static String feeling(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        int first = Integer.MAX_VALUE;
        String found = "";
        for (String word : FeelingExpressions.words()) {
            Matcher m = Pattern.compile("\\b" + word + "\\b").matcher(lower);
            if (m.find() && m.start() < first) {
                first = m.start();
                found = word;
            }
        }
        return found;
    }

    private static String caption(String text) {
        Matcher m = SCENE_CHANGE.matcher(text);
        return m.find() ? m.group() + "..." : "";
    }

    private static Scene scene(String text) {
        if (NIGHT.matcher(text).find()) {
            return Scene.NIGHT;
        }
        return INDOOR.matcher(text).find() ? Scene.INDOOR : Scene.OUTDOOR;
    }

    /**
     * The objects and animals the text names, in order, at most {@link PanelValidator#MAX_THINGS}. A
     * thing is held by the cast member who, in the same sentence, holds, carries, takes or finds it.
     */
    static List<ThingDto> things(String text, List<String> cast) {
        List<ThingDto> things = new ArrayList<>();
        for (Thing thing : ThingWords.namedIn(text)) {
            if (cast.stream().anyMatch(name -> ThingWords.isNameOf(name, thing))) {
                continue;
            }
            things.add(new ThingDto(thing, thing.canBeHeld() ? holderOf(text, thing, cast) : ""));
            if (things.size() == PanelValidator.MAX_THINGS) {
                break;
            }
        }
        return things;
    }

    private static String holderOf(String text, Thing thing, List<String> cast) {
        int at = ThingWords.firstIndex(text, thing).orElse(-1);
        Matcher sentence = SENTENCE.matcher(text);
        while (sentence.find()) {
            if (at < sentence.start() || at >= sentence.end()) {
                continue;
            }
            String before = text.substring(sentence.start(), at);
            Matcher verb = HOLDS.matcher(before);
            int verbAt = -1;
            while (verb.find()) {
                verbAt = verb.start();
            }
            return verbAt < 0 ? "" : nearestNameBefore(before.substring(0, verbAt), cast);
        }
        return "";
    }

    /** The cast member named last in {@code text}, or "" when none is. */
    private static String nearestNameBefore(String text, List<String> cast) {
        String best = "";
        int bestEnd = -1;
        for (String name : cast) {
            Matcher m = Pattern.compile("\\b" + Pattern.quote(name) + "\\b").matcher(text);
            while (m.find()) {
                if (m.end() > bestEnd) {
                    bestEnd = m.end();
                    best = name;
                }
            }
        }
        return best;
    }

    /** Notes for the idioms the demo knows, quoting the phrase exactly as the text has it. */
    static List<NoteDto> notes(String sourceText) {
        String flat = sourceText.replaceAll("\\s+", " ");
        List<NoteDto> notes = new ArrayList<>();
        IDIOMS.forEach((pattern, meaning) -> {
            Matcher m = pattern.matcher(flat);
            if (m.find() && notes.size() < 3) {
                notes.add(new NoteDto(m.group(), meaning));
            }
        });
        return notes;
    }
}
