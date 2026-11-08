package com.comicsticks.service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.comicsticks.dto.Expression;

/**
 * Matches a feeling word to the face that shows it, so that a figure labelled "scared" always looks
 * scared. Each expression is listed with the plain words that mean it.
 */
final class FeelingExpressions {

    private static final Map<String, Expression> WORDS = new LinkedHashMap<>();

    static {
        add(Expression.SCARED, "scared", "afraid", "frightened", "terrified", "fearful", "panicked");
        add(Expression.SAD, "sad", "unhappy", "upset", "lonely", "disappointed", "sorry", "miserable", "gloomy",
                "heartbroken", "homesick", "hurt");
        add(Expression.ANGRY, "angry", "cross", "furious", "annoyed", "mad", "grumpy", "jealous");
        add(Expression.SURPRISED, "surprised", "shocked", "amazed", "astonished", "startled");
        add(Expression.CONFUSED, "confused", "puzzled", "unsure", "lost", "baffled", "curious");
        add(Expression.WORRIED, "worried", "nervous", "anxious", "uneasy", "shy", "doubtful");
        add(Expression.EXCITED, "excited", "thrilled", "eager", "delighted", "overjoyed");
        add(Expression.PROUD, "proud", "confident", "boastful", "smug");
        add(Expression.EMBARRASSED, "embarrassed", "ashamed", "awkward", "guilty");
        add(Expression.DETERMINED, "determined", "brave", "bold", "focused", "stubborn");
        add(Expression.TIRED, "tired", "sleepy", "exhausted", "weary", "bored");
        add(Expression.HAPPY, "happy", "glad", "cheerful", "joyful", "pleased", "content", "relieved", "calm",
                "grateful", "thankful", "friendly", "loving");
    }

    private FeelingExpressions() {
    }

    private static void add(Expression expression, String... words) {
        for (String word : words) {
            WORDS.put(word, expression);
        }
    }

    static Optional<Expression> expressionFor(String feeling) {
        return Optional.ofNullable(WORDS.get(feeling == null ? "" : feeling.strip().toLowerCase(Locale.ROOT)));
    }

    /** Every known feeling word, for rule-based detection in the demo profile. */
    static Set<String> words() {
        return WORDS.keySet();
    }

    /** Reduces a feeling to one plain lowercase word, such as "Very scared!" to "scared". */
    static String oneWord(String feeling) {
        if (feeling == null || feeling.isBlank()) {
            return "";
        }
        String[] words = feeling.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\s-]", " ").strip().split("\\s+");
        for (String word : words) {
            if (WORDS.containsKey(word)) {
                return word;
            }
        }
        String last = words[words.length - 1];
        return last.length() > 20 ? "" : last;
    }
}
