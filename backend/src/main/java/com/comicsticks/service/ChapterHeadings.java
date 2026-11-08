package com.comicsticks.service;

import java.util.regex.Pattern;

/**
 * Recognizes the lines that start a chapter, such as "Chapter 3", "CHAPTER IV. The River",
 * "Part One" or "Prologue", while leaving ordinary sentences such as "Part one of the plan was
 * simple." alone.
 */
public final class ChapterHeadings {

    private static final int MAX_HEADING_CHARS = 80;

    private static final String NUMBER_WORD = "(?:one|two|three|four|five|six|seven|eight|nine|ten|eleven"
            + "|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|twenty|thirty|forty|fifty)";
    private static final String NUMBER = "(?:\\d{1,3}|[ivxlcdm]{1,8}|" + NUMBER_WORD + "(?:[- ]" + NUMBER_WORD + ")?)";

    /** After the number: nothing, a separator and a title, or a title in title case. */
    private static final String TITLE = "(?:\\s*[.:\\-–—]\\s*.*|\\s+\\p{Lu}.*)?";

    private static final Pattern NAMED = Pattern.compile(
            "(?i:chapter|part|book)\\s+(?i:" + NUMBER + ")\\b" + TITLE
                    + "|(?i:prologue|epilogue)\\b" + TITLE);

    /** A bare numeral on its own line, such as "IV" or "12.", which some books use for chapters. */
    private static final Pattern BARE_NUMBER = Pattern.compile("(?:[IVXLC]{1,7}|\\d{1,3})\\.?");

    /** A heading's title may end in a full stop only right after the number, as in "Chapter 4." */
    private static final Pattern SENTENCE_END = Pattern.compile(".*\\s\\S*[a-z]{2,}[.!?][\"'”’]?");

    private ChapterHeadings() {
    }

    public static boolean isChapterHeading(String line) {
        String text = line.strip();
        if (text.isEmpty() || text.length() > MAX_HEADING_CHARS || text.contains("\n")) {
            return false;
        }
        if (BARE_NUMBER.matcher(text).matches()) {
            return true;
        }
        return NAMED.matcher(text).matches() && !SENTENCE_END.matcher(text).matches();
    }
}
