package com.comicsticks.service;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.comicsticks.config.ComicProperties;
import com.comicsticks.dto.SupportLevel;
import com.comicsticks.exception.ComicGenerationException;

/**
 * Splits a story into parts, one per panel, so that each part gets its own LLM call and every
 * passage of the text gets its own picture. How much text goes under one panel depends on the
 * {@link SupportLevel}:
 *
 * <ul>
 * <li>Level 1: one panel per paragraph. A long paragraph is cut at sentences into pieces of 2 to 3
 * lines. Only a heading, or a short line of dialogue and its short reply, joins a neighbour.
 * <li>Level 2: 3 to 4 paragraphs per panel.
 * <li>Level 3: one panel per scene, breaking at {@code ***}, {@code * * *}, extra blank lines and
 * chapter headings. With no markers at all, about 6 to 10 paragraphs per panel.
 * </ul>
 *
 * <p>Groups never cross a scene break. They are spread evenly over a scene, so seven paragraphs at
 * level 2 become groups of 4 and 3 rather than 3, 3 and 1.
 *
 * <p>Every part is a contiguous span of the original text, so its {@link Part#sourceText()} is the
 * story's own words exactly as written, and the parts together cover the whole story in order.
 * Scene markers carry no words and are left out.
 *
 * <p>Text that needs more panels than {@code comic.story.max-panels} is refused, never squeezed: a
 * reader who needs a picture per paragraph must not silently lose them.
 *
 * <p>The ending gets a second panel when there is room and it has more than one sentence.
 */
@Component
public class StorySegmenter {

    /** A blank line, or several. Three or more line breaks in a row also mark a scene break. */
    private static final Pattern BLANK_LINE = Pattern.compile("\\n[ \\t\\x0B\\f]*\\n\\s*");
    private static final Pattern NEWLINE = Pattern.compile("\\n\\s*");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    /** A line such as {@code ***}, {@code * * *}, {@code #} or {@code ~~~}, with no words. */
    private static final Pattern SCENE_MARKER = Pattern.compile("[*#~=_\\-•·\\s]+");
    private static final Pattern QUOTE = Pattern.compile("[\"“”‘’]|(?:^|\\s)'");

    /**
     * One LLM call's worth of story.
     *
     * @param text       the part with its whitespace collapsed, as sent to the LLM
     * @param panels     how many panels the part gets: 1, or 2 for the ending
     * @param sourceText the part exactly as it appears in the story, shown under its last panel
     */
    public record Part(String text, int panels, String sourceText) {
    }

    /** A stretch of the story, as offsets into the line-ending-normalized text. */
    private record Span(int start, int end) {
        int length() {
            return end - start;
        }

        Span to(Span later) {
            return new Span(start, later.end);
        }
    }

    /**
     * A paragraph, or a piece of one.
     *
     * @param sceneBreakBefore a scene marker, extra blank lines or a chapter heading comes before it
     * @param heading          a line such as a title or chapter heading, which belongs with what follows
     */
    private record Block(Span span, boolean sceneBreakBefore, boolean heading) {
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
        return split(story, SupportLevel.PARAGRAPH);
    }

    public List<Part> split(String story, SupportLevel level) {
        String text = normalizeLineEndings(story);
        List<Block> blocks = blocks(text);
        List<Span> spans = switch (level) {
            case PARAGRAPH -> paragraphParts(text, blocks);
            case GROUP -> groupedParts(text, attachHeadings(blocks), limits.level2ParagraphsPerPanel(),
                    limits.level2ParagraphsPerPanel() + 1);
            case SCENE -> sceneParts(text, attachHeadings(blocks));
        };
        if (spans.size() > limits.maxPanels()) {
            throw new ComicGenerationException(HttpStatus.CONTENT_TOO_LARGE, ("This text needs %d panels at support "
                    + "level %d, but one comic can have at most %d. Split it into chapters, or paste a shorter piece.")
                    .formatted(spans.size(), level.number(), limits.maxPanels()));
        }
        return toParts(text, spans);
    }

    // --- Reading the text ------------------------------------------------------------------

    private List<Block> blocks(String text) {
        // Text with no blank lines at all usually has one paragraph per line.
        Pattern separator = BLANK_LINE.matcher(text).find() ? BLANK_LINE : NEWLINE;
        List<Block> blocks = new ArrayList<>();
        Matcher matcher = separator.matcher(text);
        int start = 0;
        boolean sceneBreak = false;
        while (true) {
            boolean found = matcher.find();
            int end = found ? matcher.start() : text.length();
            Span span = trimmed(text, start, end);
            if (span != null) {
                String paragraph = text.substring(span.start(), span.end());
                if (SCENE_MARKER.matcher(paragraph).matches()) {
                    sceneBreak = true;
                } else {
                    boolean chapter = ChapterHeadings.isChapterHeading(paragraph);
                    boolean first = blocks.isEmpty();
                    blocks.add(new Block(span, !first && (sceneBreak || chapter), chapter || isHeading(paragraph)));
                    sceneBreak = false;
                }
            }
            if (!found) {
                return blocks;
            }
            if (lineBreaks(matcher.group()) >= 3) {
                sceneBreak = true;
            }
            start = matcher.end();
        }
    }

    /** A short line that does not end like a sentence or open with a quote, such as a title. */
    private boolean isHeading(String paragraph) {
        if (paragraph.length() >= limits.shortLineChars() || paragraph.contains("\n")) {
            return false;
        }
        char last = paragraph.charAt(paragraph.length() - 1);
        return Character.isLetterOrDigit(last) && !QUOTE.matcher(paragraph.substring(0, 1)).find();
    }

    private static long lineBreaks(String separator) {
        return separator.chars().filter(c -> c == '\n').count();
    }

    private static Span trimmed(String text, int start, int end) {
        while (start < end && Character.isWhitespace(text.charAt(start))) {
            start++;
        }
        while (end > start && Character.isWhitespace(text.charAt(end - 1))) {
            end--;
        }
        return start < end ? new Span(start, end) : null;
    }

    // --- Level 1 ---------------------------------------------------------------------------

    private List<Span> paragraphParts(String text, List<Block> blocks) {
        List<Block> pieces = new ArrayList<>();
        for (Block block : blocks) {
            List<Span> cut = cutAtSentences(text, block.span(), limits.level1MaxPartChars());
            for (int i = 0; i < cut.size(); i++) {
                pieces.add(new Block(cut.get(i), i == 0 && block.sceneBreakBefore(), cut.size() == 1 && block.heading()));
            }
        }

        List<Span> parts = new ArrayList<>();
        for (int i = 0; i < pieces.size(); i++) {
            Block current = pieces.get(i);
            Span span = current.span();
            // A heading belongs with what follows it, even across several heading lines.
            while (current.heading() && i + 1 < pieces.size()) {
                current = pieces.get(++i);
                span = span.to(current.span());
            }
            if (current.heading() && !parts.isEmpty()) {
                // A heading with nothing after it, such as "The End".
                parts.set(parts.size() - 1, parts.getLast().to(span));
                continue;
            }
            // A short line of dialogue and its short reply make one exchange.
            if (i + 1 < pieces.size() && isShortDialogue(text, current) && isShortDialogue(text, pieces.get(i + 1))
                    && !pieces.get(i + 1).sceneBreakBefore() && span.length() == current.span().length()) {
                span = span.to(pieces.get(++i).span());
            }
            parts.add(span);
        }
        return parts;
    }

    private boolean isShortDialogue(String text, Block block) {
        return !block.heading() && block.span().length() < limits.shortLineChars()
                && QUOTE.matcher(text.substring(block.span().start(), block.span().end())).find();
    }

    // --- Levels 2 and 3 --------------------------------------------------------------------

    /** Joins each heading to the paragraph after it. A chapter heading still starts a new scene. */
    private static List<Block> attachHeadings(List<Block> blocks) {
        List<Block> joined = new ArrayList<>();
        Block pending = null;
        for (Block block : blocks) {
            if (pending != null) {
                block = new Block(pending.span().to(block.span()), pending.sceneBreakBefore() || block.sceneBreakBefore(),
                        block.heading());
                pending = null;
            }
            if (block.heading()) {
                pending = block;
            } else {
                joined.add(block);
            }
        }
        if (pending != null) {
            if (joined.isEmpty()) {
                joined.add(pending);
            } else {
                Block last = joined.removeLast();
                joined.add(new Block(last.span().to(pending.span()), last.sceneBreakBefore(), false));
            }
        }
        return joined;
    }

    private List<Span> sceneParts(String text, List<Block> blocks) {
        boolean hasScenes = blocks.stream().anyMatch(Block::sceneBreakBefore);
        if (hasScenes) {
            // One panel per scene, unless the scene is too long for one picture.
            return groupedParts(text, blocks, Integer.MAX_VALUE, limits.level3MaxParagraphs());
        }
        int target = Math.round((limits.level3MinParagraphs() + limits.level3MaxParagraphs()) / 2f);
        return groupedParts(text, blocks, target, limits.level3MaxParagraphs());
    }

    /** Spreads each scene's paragraphs evenly over groups of about {@code target}, never above {@code max}. */
    private List<Span> groupedParts(String text, List<Block> blocks, int target, int max) {
        List<Span> parts = new ArrayList<>();
        for (List<Block> scene : scenes(blocks)) {
            int n = scene.size();
            int groups = Math.max(Math.ceilDiv(n, max), Math.max(1, Math.round((float) n / target)));
            for (int g = 0; g < groups; g++) {
                List<Block> group = scene.subList(g * n / groups, (g + 1) * n / groups);
                parts.addAll(withinCallLimit(text, group));
            }
        }
        return parts;
    }

    private static List<List<Block>> scenes(List<Block> blocks) {
        List<List<Block>> scenes = new ArrayList<>();
        for (Block block : blocks) {
            if (scenes.isEmpty() || block.sceneBreakBefore()) {
                scenes.add(new ArrayList<>());
            }
            scenes.getLast().add(block);
        }
        return scenes;
    }

    /** Splits a group at paragraphs, then sentences, if it is too long for one LLM call. */
    private List<Span> withinCallLimit(String text, List<Block> group) {
        List<Span> parts = new ArrayList<>();
        Span current = null;
        for (Block block : group) {
            if (current != null && block.span().end() - current.start() > limits.maxPartChars()) {
                parts.add(current);
                current = null;
            }
            if (current == null && block.span().length() > limits.maxPartChars()) {
                parts.addAll(cutAtSentences(text, block.span(), limits.maxPartChars()));
                continue;
            }
            current = current == null ? block.span() : current.to(block.span());
        }
        if (current != null) {
            parts.add(current);
        }
        return parts;
    }

    // --- Shared ----------------------------------------------------------------------------

    private static List<Span> cutAtSentences(String text, Span paragraph, int maxChars) {
        if (paragraph.length() <= maxChars) {
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
            if (previous > pieceStart && end - pieceStart > maxChars) {
                addTrimmed(text, paragraph.start() + pieceStart, paragraph.start() + previous, pieces);
                pieceStart = previous;
            }
        }
        addTrimmed(text, paragraph.start() + pieceStart, paragraph.end(), pieces);
        return pieces;
    }

    private static void addTrimmed(String text, int start, int end, List<Span> into) {
        Span span = trimmed(text, start, end);
        if (span != null) {
            into.add(span);
        }
    }

    private static int sentenceCount(String text) {
        BreakIterator sentences = BreakIterator.getSentenceInstance(Locale.ROOT);
        sentences.setText(text);
        int count = 0;
        sentences.first();
        while (sentences.next() != BreakIterator.DONE) {
            count++;
        }
        return count;
    }

    /** One panel each, and a second for an ending of more than one sentence when there is room. */
    private List<Part> toParts(String text, List<Span> spans) {
        List<Part> parts = new ArrayList<>();
        for (int i = 0; i < spans.size(); i++) {
            String source = text.substring(spans.get(i).start(), spans.get(i).end());
            String flat = WHITESPACE.matcher(source).replaceAll(" ");
            boolean ending = i == spans.size() - 1 && spans.size() < limits.maxPanels() && sentenceCount(flat) > 1;
            parts.add(new Part(flat, ending ? 2 : 1, source));
        }
        return parts;
    }
}
