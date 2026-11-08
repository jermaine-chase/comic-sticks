package com.comicsticks.service;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
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
 * <p>Every part is a contiguous span of the original text, so its {@link Part#sourceText()} is the
 * story's own words, exactly as written, and the parts together cover the whole story in order.
 *
 * <p>The ending gets special care. A part with one panel shows only its opening, so an ending
 * merged into a longer part would vanish: the final part stays separate and gets a second panel
 * whenever one is spare.
 */
@Component
public class StorySegmenter {

    private static final Pattern BLANK_LINE = Pattern.compile("\\n[ \\t\\x0B\\f]*\\n\\s*");
    private static final Pattern NEWLINE = Pattern.compile("\\n\\s*");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    /**
     * One LLM call's worth of story.
     *
     * @param text       the part with its whitespace collapsed, as sent to the LLM
     * @param panels     the most panels that call may produce
     * @param sourceText the part exactly as it appears in the story, shown under its last panel
     */
    public record Part(String text, int panels, String sourceText) {
    }

    /** A stretch of the story, as offsets into the line-ending-normalized text. */
    private record Span(int start, int end) {
        int length() {
            return end - start;
        }
    }

    private final ComicProperties.Story limits;

    public StorySegmenter(ComicProperties properties) {
        this.limits = properties.story();
    }

    /** Line endings are the only change made to the story's text. */
    static String normalizeLineEndings(String story) {
        return story.replace("\r\n", "\n").replace('\r', '\n');
    }

    public List<Part> split(String story) {
        String text = normalizeLineEndings(story);
        List<Span> spans = new ArrayList<>();
        for (Span paragraph : paragraphs(text)) {
            spans.addAll(cutAtSentences(text, paragraph));
        }
        spans = joinShortParts(spans);
        while (spans.size() > limits.maxPanels()) {
            mergeSmallestAdjacentPair(spans);
        }
        return assignPanels(text, spans);
    }

    private static List<Span> paragraphs(String text) {
        // Text with no blank lines at all usually has one paragraph per line.
        Pattern separator = BLANK_LINE.matcher(text).find() ? BLANK_LINE : NEWLINE;
        List<Span> paragraphs = new ArrayList<>();
        Matcher matcher = separator.matcher(text);
        int start = 0;
        while (matcher.find()) {
            addTrimmed(text, start, matcher.start(), paragraphs);
            start = matcher.end();
        }
        addTrimmed(text, start, text.length(), paragraphs);
        return paragraphs;
    }

    private static void addTrimmed(String text, int start, int end, List<Span> into) {
        while (start < end && Character.isWhitespace(text.charAt(start))) {
            start++;
        }
        while (end > start && Character.isWhitespace(text.charAt(end - 1))) {
            end--;
        }
        if (start < end) {
            into.add(new Span(start, end));
        }
    }

    private List<Span> cutAtSentences(String text, Span paragraph) {
        if (paragraph.length() <= limits.maxPartChars()) {
            return List.of(paragraph);
        }
        // Line breaks inside a paragraph must not end a sentence. Swapping each whitespace
        // character for a space keeps every offset the same.
        String flat = text.substring(paragraph.start(), paragraph.end()).replaceAll("\\s", " ");
        List<Span> pieces = new ArrayList<>();
        BreakIterator sentences = BreakIterator.getSentenceInstance(Locale.ROOT);
        sentences.setText(flat);
        int pieceStart = 0;
        int previous = sentences.first();
        for (int end = sentences.next(); end != BreakIterator.DONE; previous = end, end = sentences.next()) {
            if (previous > pieceStart && end - pieceStart > limits.maxPartChars()) {
                addTrimmed(text, paragraph.start() + pieceStart, paragraph.start() + previous, pieces);
                pieceStart = previous;
            }
        }
        addTrimmed(text, paragraph.start() + pieceStart, paragraph.end(), pieces);
        return pieces;
    }

    private List<Span> joinShortParts(List<Span> spans) {
        List<Span> joined = new ArrayList<>();
        Span pending = null;
        for (Span span : spans) {
            pending = pending == null ? span : new Span(pending.start(), span.end());
            if (pending.length() >= limits.minPartChars()) {
                joined.add(pending);
                pending = null;
            }
        }
        if (pending != null) {
            // A closing paragraph is often short but carries the ending, so only a line as
            // slight as "The end." joins the part before it.
            if (joined.isEmpty() || pending.length() >= limits.minPartChars() / 3) {
                joined.add(pending);
            } else {
                joined.set(joined.size() - 1, new Span(joined.getLast().start(), pending.end()));
            }
        }
        return joined;
    }

    /** Leaves the final part alone, except when only two parts remain. */
    private static void mergeSmallestAdjacentPair(List<Span> spans) {
        int smallest = 0;
        for (int i = 1; i < spans.size() - 2; i++) {
            if (pairLength(spans, i) < pairLength(spans, smallest)) {
                smallest = i;
            }
        }
        Span next = spans.remove(smallest + 1);
        spans.set(smallest, new Span(spans.get(smallest).start(), next.end()));
    }

    private static int pairLength(List<Span> spans, int i) {
        return spans.get(i).length() + spans.get(i + 1).length();
    }

    /** Shares panels in proportion to length, with at least one each and two for the ending. */
    private List<Part> assignPanels(String text, List<Span> spans) {
        int totalChars = spans.stream().mapToInt(Span::length).sum();
        int wanted = Math.ceilDiv(totalChars, limits.charsPerPanel());
        int totalPanels = Math.clamp(wanted, spans.size(), limits.maxPanels());

        int[] panels = new int[spans.size()];
        int[] minimums = new int[spans.size()];
        double[] exact = new double[spans.size()];
        Arrays.fill(minimums, 1);
        if (totalPanels > spans.size()) {
            minimums[minimums.length - 1] = 2;
        }
        for (int i = 0; i < panels.length; i++) {
            exact[i] = (double) totalPanels * spans.get(i).length() / totalChars;
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
            String source = text.substring(spans.get(i).start(), spans.get(i).end());
            parts.add(new Part(WHITESPACE.matcher(source).replaceAll(" "), panels[i], source));
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
