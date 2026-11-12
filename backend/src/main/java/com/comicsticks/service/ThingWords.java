package com.comicsticks.service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.comicsticks.dto.Thing;

/**
 * Finds the words that name a {@link Thing} in story text: whole words only, ignoring case, with
 * plurals ending in "s" or "es". A thing is only drawn when its panel's text names it.
 */
final class ThingWords {

    private static final Map<Thing, Pattern> PATTERNS = new EnumMap<>(Thing.class);

    static {
        for (Thing thing : Thing.values()) {
            String words = String.join("|", thing.words().stream()
                    .map(word -> Pattern.quote(word).replace(" ", "\\E\\s+\\Q"))
                    .toList());
            PATTERNS.put(thing, Pattern.compile("(?<![\\p{L}\\p{N}])(?:" + words + ")(?:e?s)?(?![\\p{L}\\p{N}])",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
        }
    }

    private ThingWords() {
    }

    /** Whether {@code text} uses one of the words for {@code thing}. */
    static boolean names(String text, Thing thing) {
        return text != null && thing != null && PATTERNS.get(thing).matcher(text).find();
    }

    /** Where {@code text} first names {@code thing}, if it does. */
    static OptionalInt firstIndex(String text, Thing thing) {
        if (text == null || thing == null) {
            return OptionalInt.empty();
        }
        Matcher matcher = PATTERNS.get(thing).matcher(text);
        return matcher.find() ? OptionalInt.of(matcher.start()) : OptionalInt.empty();
    }

    /** Every thing the text names, in the order the text first names them. */
    static List<Thing> namedIn(String text) {
        return Arrays.stream(Thing.values())
                .filter(thing -> names(text, thing))
                .sorted(Comparator.comparingInt(thing -> firstIndex(text, thing).orElse(Integer.MAX_VALUE)))
                .toList();
    }

    /** Whether {@code name} is itself one of the thing's words, as for an animal character called "Dog". */
    static boolean isNameOf(String name, Thing thing) {
        return name != null && thing != null
                && PATTERNS.get(thing).matcher(name.strip()).matches();
    }
}
