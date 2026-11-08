package com.comicsticks.service;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.comicsticks.config.ComicProperties;

/**
 * Splits a story into paragraph-based parts and shares the comic's panels between them, so that
 * each part gets its own LLM call and no part of the story can be crowded out by another.
 *
 * <p>Paragraphs are the unit, with three adjustments: a paragraph too long for one call is cut at
 * sentence boundaries, one too short to be worth a panel (a chapter heading, say) joins its
 * neighbour, and adjacent parts merge if there would otherwise be more parts than panels.
 *
 * <p>The ending gets special care. A part with one panel shows only its opening, so an ending
 * merged into a longer part would vanish: the final part stays separate and gets a second panel
 * whenever one is spare.
 */
@Component
public class StorySegmenter {

    private static final Pattern BLANK_LINE = Pattern.compile("\\n\\s*\\n");
    private static final Pattern NEWLINE = Pattern.compile("\\n");

    /** One LLM call's worth of story, and the most panels that call may produce. */
    public record Part(String text, int panels) {
    }

    private final ComicProperties.Story limits;

    public StorySegmenter(ComicProperties properties) {
        this.limits = properties.story();
    }

    public List<Part> split(String story) {
        List<String> texts = new ArrayList<>();
        for (String paragraph : paragraphs(story)) {
            texts.addAll(cutAtSentences(paragraph));
        }
        texts = joinShortParts(texts);
        while (texts.size() > limits.maxPanels()) {
            mergeSmallestAdjacentPair(texts);
        }
        return assignPanels(texts);
    }

    private static List<String> paragraphs(String story) {
        String text = story.replace("\r\n", "\n").strip();
        // Text with no blank lines at all usually has one paragraph per line.
        Pattern separator = BLANK_LINE.matcher(text).find() ? BLANK_LINE : NEWLINE;
        return separator.splitAsStream(text)
                .map(paragraph -> paragraph.strip().replaceAll("\\s+", " "))
                .filter(paragraph -> !paragraph.isEmpty())
                .toList();
    }

    private List<String> cutAtSentences(String paragraph) {
        if (paragraph.length() <= limits.maxPartChars()) {
            return List.of(paragraph);
        }
        List<String> pieces = new ArrayList<>();
        StringBuilder piece = new StringBuilder();
        BreakIterator sentences = BreakIterator.getSentenceInstance(Locale.ROOT);
        sentences.setText(paragraph);
        int start = sentences.first();
        for (int end = sentences.next(); end != BreakIterator.DONE; start = end, end = sentences.next()) {
            String sentence = paragraph.substring(start, end);
            if (!piece.isEmpty() && piece.length() + sentence.length() > limits.maxPartChars()) {
                pieces.add(piece.toString().strip());
                piece.setLength(0);
            }
            piece.append(sentence);
        }
        if (!piece.isEmpty()) {
            pieces.add(piece.toString().strip());
        }
        return pieces;
    }

    private List<String> joinShortParts(List<String> texts) {
        List<String> joined = new ArrayList<>();
        String pending = "";
        for (String text : texts) {
            pending = pending.isEmpty() ? text : pending + " " + text;
            if (pending.length() >= limits.minPartChars()) {
                joined.add(pending);
                pending = "";
            }
        }
        if (!pending.isEmpty()) {
            // A closing paragraph is often short but carries the ending, so only a line as
            // slight as "The end." joins the part before it.
            if (joined.isEmpty() || pending.length() >= limits.minPartChars() / 3) {
                joined.add(pending);
            } else {
                joined.set(joined.size() - 1, joined.getLast() + " " + pending);
            }
        }
        return joined;
    }

    /** Leaves the final part alone, except when only two parts remain. */
    private static void mergeSmallestAdjacentPair(List<String> texts) {
        int smallest = 0;
        for (int i = 1; i < texts.size() - 2; i++) {
            int size = texts.get(i).length() + texts.get(i + 1).length();
            if (size < texts.get(smallest).length() + texts.get(smallest + 1).length()) {
                smallest = i;
            }
        }
        texts.set(smallest, texts.get(smallest) + " " + texts.remove(smallest + 1));
    }

    /** Shares panels in proportion to length, with at least one each and two for the ending. */
    private List<Part> assignPanels(List<String> texts) {
        int totalChars = texts.stream().mapToInt(String::length).sum();
        int wanted = Math.ceilDiv(totalChars, limits.charsPerPanel());
        int totalPanels = Math.clamp(wanted, texts.size(), limits.maxPanels());

        int[] panels = new int[texts.size()];
        int[] minimums = new int[texts.size()];
        double[] exact = new double[texts.size()];
        Arrays.fill(minimums, 1);
        if (totalPanels > texts.size()) {
            minimums[minimums.length - 1] = 2;
        }
        for (int i = 0; i < panels.length; i++) {
            exact[i] = (double) totalPanels * texts.get(i).length() / totalChars;
            panels[i] = Math.max(minimums[i], (int) exact[i]);
        }
        int assigned = Arrays.stream(panels).sum();
        for (; assigned < totalPanels; assigned++) {
            panels[mostUnderserved(panels, exact)]++;
        }
        for (; assigned > totalPanels; assigned--) {
            panels[mostOverserved(panels, minimums, exact)]--;
        }

        List<Part> parts = new ArrayList<>();
        for (int i = 0; i < panels.length; i++) {
            parts.add(new Part(texts.get(i), panels[i]));
        }
        return parts;
    }

    private static int mostUnderserved(int[] panels, double[] exact) {
        int best = 0;
        for (int i = 1; i < panels.length; i++) {
            if (exact[i] - panels[i] > exact[best] - panels[best]) {
                best = i;
            }
        }
        return best;
    }

    private static int mostOverserved(int[] panels, int[] minimums, double[] exact) {
        int best = -1;
        for (int i = 0; i < panels.length; i++) {
            if (panels[i] > minimums[i] && (best < 0 || panels[i] - exact[i] > panels[best] - exact[best])) {
                best = i;
            }
        }
        return best;
    }
}
