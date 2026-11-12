package com.comicsticks.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import com.comicsticks.dto.ChapterBookends.AnswerChoice;
import com.comicsticks.dto.ChapterBookends.Question;
import com.comicsticks.dto.ChapterBookends.RecapItem;
import com.comicsticks.dto.ChapterBookends.WhosWho;
import com.comicsticks.dto.CharacterDesign;
import com.comicsticks.dto.CharacterDto;
import com.comicsticks.dto.ComicStoryResponse;
import com.comicsticks.dto.Expression;
import com.comicsticks.dto.PanelDto;
import com.comicsticks.dto.Scene;

/**
 * The rules for a chapter's opening and closing, shared by the LLM and demo writers. Everything the
 * reader is shown is checked against the chapter itself: recap thumbnails are real panels, the
 * main character is someone in the chapter, and every answer choice is a character or panel the
 * reader has seen. The closing sentence is written here, not by the LLM, so it is always there
 * and always plain: "At the end, Mole feels proud."
 */
final class BookendsRules {

    static final int MIN_RECAP = 3;
    static final int MAX_RECAP = 5;
    static final int MAX_QUESTIONS = 3;
    private static final int MAX_WORDS = 25;
    private static final String FALLBACK_FEELING = "calm";
    private static final Pattern FIRST_SENTENCE = Pattern.compile("^.*?[.!?](?=\\s|[\"\u201D']?\\s|$)[\"\u201D']?");

    private BookendsRules() {
    }

    // --- Who's who ------------------------------------------------------------------------

    /** Everyone drawn in the chapter, in order of first appearance. */
    static List<String> cast(ComicStoryResponse story) {
        Set<String> cast = new LinkedHashSet<>();
        for (PanelDto panel : story.panels()) {
            panel.characters().stream().map(CharacterDto::name).forEach(cast::add);
        }
        return List.copyOf(cast);
    }

    static WhosWho whosWho(ComicStoryResponse story, String whereWeAre) {
        String line = plain(whereWeAre);
        return new WhosWho(cast(story), line.isEmpty() ? fallbackWhere(story) : line);
    }

    /** "We are at the river bank." from the first panel's place, or else from its scene. */
    static String fallbackWhere(ComicStoryResponse story) {
        if (story.panels().isEmpty()) {
            return "";
        }
        PanelDto first = story.panels().getFirst();
        String place = first.place() == null ? "" : first.place();
        if (!place.isEmpty()) {
            String name = place.startsWith("The ") ? "the " + place.substring(4) : place;
            return "We are at " + name + (first.scene() == Scene.NIGHT ? ", at night." : ".");
        }
        return switch (first.scene() == null ? Scene.PLAIN : first.scene()) {
            case INDOOR -> "We are inside.";
            case OUTDOOR -> "We are outside, in the daytime.";
            case NIGHT -> "It is night time.";
            case PLAIN -> "";
        };
    }

    // --- Recap ----------------------------------------------------------------------------

    /** The panels that carry the story's text, which are the ones a recap can point back to. */
    static List<PanelDto> textPanels(ComicStoryResponse story) {
        return story.panels().stream().filter(p -> p.sourceText() != null && !p.sourceText().isBlank()).toList();
    }

    /**
     * A numbered summary of the chapter for the recap and questions calls: the start of each text
     * panel, sampled evenly if the whole would be too long.
     */
    static String panelSummary(ComicStoryResponse story, int maxChars, int panelChars) {
        List<PanelDto> panels = textPanels(story);
        int perLine = panelChars + 12;
        int keep = Math.max(1, Math.min(panels.size(), maxChars / perLine));
        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < keep; i++) {
            PanelDto panel = panels.get(keep == panels.size() ? i : i * (panels.size() - 1) / Math.max(1, keep - 1));
            String text = panel.sourceText().replaceAll("\\s+", " ").strip();
            summary.append("Panel ").append(panel.panelNumber()).append(": ")
                    .append(text.length() <= panelChars ? text : text.substring(0, panelChars).strip() + "...")
                    .append('\n');
        }
        return summary.toString();
    }

    /**
     * Keeps the suggested recap lines that point at real panels, fills up from the text when there
     * are too few, and adds the closing feeling line, for 3 to 5 lines in story order.
     */
    static List<RecapItem> recap(ComicStoryResponse story, List<RecapItem> suggested, String mainCharacter,
            String feeling) {
        Set<Integer> panelNumbers = new LinkedHashSet<>();
        textPanels(story).forEach(p -> panelNumbers.add(p.panelNumber()));
        Map<Integer, RecapItem> kept = new LinkedHashMap<>();
        for (RecapItem item : suggested == null ? List.<RecapItem>of() : suggested) {
            if (item == null || item.panelNumber() == null || !panelNumbers.contains(item.panelNumber())
                    || plain(item.sentence()).isEmpty() || kept.containsKey(item.panelNumber())
                    || kept.size() == MAX_RECAP - 1) {
                continue;
            }
            kept.put(item.panelNumber(), new RecapItem(sentence(item.sentence()), item.panelNumber(), null, null));
        }
        if (kept.size() < MIN_RECAP - 1) {
            for (RecapItem item : fallbackRecap(story, MIN_RECAP - 1)) {
                if (kept.size() < MIN_RECAP - 1) {
                    kept.putIfAbsent(item.panelNumber(), item);
                }
            }
        }
        List<RecapItem> recap = new ArrayList<>(kept.values());
        recap.sort(Comparator.comparing(RecapItem::panelNumber));
        if (!mainCharacter.isEmpty()) {
            Expression face = FeelingExpressions.expressionFor(feeling).orElse(Expression.NEUTRAL);
            recap.add(new RecapItem("At the end, %s feels %s.".formatted(mainCharacter, feeling), null, mainCharacter,
                    face));
        }
        return recap;
    }

    /** The first sentence of evenly spaced text panels. */
    static List<RecapItem> fallbackRecap(ComicStoryResponse story, int count) {
        List<PanelDto> panels = textPanels(story);
        List<RecapItem> recap = new ArrayList<>();
        int n = Math.min(count, panels.size());
        for (int i = 0; i < n; i++) {
            PanelDto panel = panels.get(n == 1 ? 0 : i * (panels.size() - 1) / (n - 1));
            recap.add(new RecapItem(sentence(firstSentence(panel.sourceText())), panel.panelNumber(), null, null));
        }
        return recap;
    }

    /** The suggested main character if they are in the chapter, otherwise whoever appears most. */
    static String mainCharacter(ComicStoryResponse story, String suggested) {
        List<String> cast = cast(story);
        if (suggested != null) {
            List<CharacterDesign> asDesigns = cast.stream().map(name -> SheetRules.defaultDesign(name, 0)).toList();
            var match = SheetRules.find(asDesigns, suggested);
            if (match.isPresent()) {
                return match.get().name();
            }
        }
        Map<String, Integer> counts = new LinkedHashMap<>();
        story.panels().forEach(p -> p.characters().forEach(c -> counts.merge(c.name(), 1, Integer::sum)));
        return counts.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("");
    }

    /** One plain word: the suggestion, else the main character's last feeling, else "calm". */
    static String endingFeeling(ComicStoryResponse story, String mainCharacter, String suggested) {
        String feeling = FeelingExpressions.oneWord(suggested);
        if (!feeling.isEmpty()) {
            return feeling;
        }
        for (PanelDto panel : story.panels().reversed()) {
            for (CharacterDto c : panel.characters()) {
                if (c.name().equals(mainCharacter) && c.feeling() != null && !c.feeling().isBlank()) {
                    return c.feeling();
                }
            }
        }
        return FALLBACK_FEELING;
    }

    // --- Questions ------------------------------------------------------------------------

    /**
     * A "who" question whose choices are the answer and up to two others from the chapter, in name
     * order. Null when the answer is not in the chapter or there is nobody else to choose.
     */
    static Question whoQuestion(ComicStoryResponse story, String question, String answer) {
        List<String> cast = cast(story);
        int at = cast.indexOf(answer);
        if (at < 0 || cast.size() < 2 || plain(question).isEmpty()) {
            return null;
        }
        Set<String> names = new LinkedHashSet<>(List.of(answer));
        for (int step = 1; names.size() < 3 && step < cast.size(); step++) {
            names.add(cast.get((at + step) % cast.size()));
        }
        List<String> sorted = names.stream().sorted().toList();
        return new Question(questionText(question),
                sorted.stream().map(name -> new AnswerChoice(name, null)).toList(), sorted.indexOf(answer));
    }

    /**
     * A "which picture" question whose choices are the answer and two panels well apart from it, in
     * story order. Null when the panel is not a text panel or the chapter is too short.
     */
    static Question pictureQuestion(ComicStoryResponse story, String question, Integer answerPanel) {
        List<Integer> numbers = textPanels(story).stream().map(PanelDto::panelNumber).toList();
        int at = answerPanel == null ? -1 : numbers.indexOf(answerPanel);
        if (at < 0 || numbers.size() < 3 || plain(question).isEmpty()) {
            return null;
        }
        int third = Math.max(1, numbers.size() / 3);
        Set<Integer> choices = new LinkedHashSet<>(List.of(answerPanel));
        choices.add(numbers.get((at + third) % numbers.size()));
        choices.add(numbers.get((at + 2 * third) % numbers.size()));
        List<Integer> sorted = choices.stream().sorted().toList();
        if (sorted.size() < 2) {
            return null;
        }
        return new Question(questionText(question),
                sorted.stream().map(n -> new AnswerChoice(null, n)).toList(), sorted.indexOf(answerPanel));
    }

    static List<Question> limit(List<Question> questions) {
        return questions.stream().filter(Objects::nonNull).limit(MAX_QUESTIONS).toList();
    }

    // --- Text -----------------------------------------------------------------------------

    static String firstSentence(String text) {
        String flat = plain(text);
        var m = FIRST_SENTENCE.matcher(flat);
        return m.find() ? m.group() : flat;
    }

    /** A short, plain sentence: at most 25 words, starting with a capital and ending with a stop. */
    static String sentence(String text) {
        String[] words = plain(text).split(" ");
        String line = words.length <= MAX_WORDS ? plain(text)
                : String.join(" ", List.of(words).subList(0, MAX_WORDS)).replaceAll("[,;:]$", "") + "...";
        if (line.isEmpty()) {
            return line;
        }
        line = Character.toUpperCase(line.charAt(0)) + line.substring(1);
        return line.matches(".*[.!?\"\u201D']$") || line.endsWith("...") ? line : line + ".";
    }

    private static String questionText(String question) {
        String line = plain(question);
        line = Character.toUpperCase(line.charAt(0)) + line.substring(1);
        return line.endsWith("?") ? line : line.replaceAll("[.!]+$", "") + "?";
    }

    private static String plain(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").strip();
    }
}
