package com.comicsticks.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Checks whether words appear in a text, ignoring the differences a copy can innocently introduce:
 * curly versus straight quotes, letter case and whitespace.
 */
final class TextMatch {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern SINGLE_QUOTES = Pattern.compile("[‘’‚‛′`´]");
    private static final Pattern DOUBLE_QUOTES = Pattern.compile("[“”„‟″«»]");
    private static final Pattern DASHES = Pattern.compile("[–—]");
    /** Punctuation a bubble may drop or change at its ends, as in "Go," becoming "Go." */
    private static final Pattern EDGE_PUNCTUATION = Pattern.compile("^[\\p{Punct}\\s]+|[\\p{Punct}\\s]+$");
    private static final Pattern DOUBLE_QUOTED = Pattern.compile("\"([^\"]*)\"");

    private TextMatch() {
    }

    static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String plain = SINGLE_QUOTES.matcher(text).replaceAll("'");
        plain = DOUBLE_QUOTES.matcher(plain).replaceAll("\"");
        plain = DASHES.matcher(plain).replaceAll("-").replace("…", "...").replace(' ', ' ');
        return WHITESPACE.matcher(plain).replaceAll(" ").strip().toLowerCase(Locale.ROOT);
    }

    /** The words, normalized, without punctuation at either end. */
    static String core(String words) {
        return EDGE_PUNCTUATION.matcher(normalize(words)).replaceAll("");
    }

    /** Whether {@code words} appear in {@code text}, after normalizing both. */
    static boolean contains(String text, String words) {
        String core = core(words);
        return !core.isEmpty() && normalize(text).contains(core);
    }

    /**
     * Whether {@code words} were spoken in {@code text}. When the text marks speech with double
     * quotes, the words must fall inside a quotation. Texts that use single quotes, or none, cannot
     * be told apart from apostrophes reliably, so for them the words need only appear in the text.
     */
    static boolean isSpokenIn(String text, String words) {
        String core = core(words);
        if (core.isEmpty()) {
            return false;
        }
        List<String> quotations = quotations(text);
        if (quotations.isEmpty()) {
            return normalize(text).contains(core);
        }
        return quotations.stream().anyMatch(q -> q.contains(core));
    }

    private static List<String> quotations(String text) {
        List<String> quotations = new ArrayList<>();
        Matcher matcher = DOUBLE_QUOTED.matcher(normalize(text));
        while (matcher.find()) {
            quotations.add(matcher.group(1));
        }
        return quotations;
    }
}
