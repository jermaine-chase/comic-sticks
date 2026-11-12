package com.comicsticks.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.comicsticks.dto.Accessory;
import com.comicsticks.dto.AnimalTail;
import com.comicsticks.dto.Background;
import com.comicsticks.dto.CharacterDesign;
import com.comicsticks.dto.CharacterKind;
import com.comicsticks.dto.CharacterSheet;
import com.comicsticks.dto.Ears;
import com.comicsticks.dto.Hair;
import com.comicsticks.dto.Hat;
import com.comicsticks.dto.PlaceDto;
import com.comicsticks.dto.Snout;
import com.comicsticks.dto.Species;

/**
 * The demo profile's character pass, by simple rules: capitalized words used at least twice (or
 * before "said") are characters, animal names make animals of that species (other parts are handed
 * out in turn), people get designs handed out in turn, and
 * place words such as "river" or "burrow" become places.
 */
@Service
@Profile("demo")
public class DemoCharacterFinder implements CharacterFinder {

    static final Pattern NAME = Pattern.compile("\\b[A-Z][a-z]{2,}\\b");
    static final List<String> NOT_NAMES = List.of("The", "Then", "When", "She", "They", "But", "And", "Once",
            "That", "There", "This", "His", "Her", "After", "Before", "Suddenly", "Chapter", "Later", "Meanwhile",
            "Back", "Soon", "What", "Where", "Why", "How", "Yes", "Look", "Come", "Over", "Here", "Now", "Who",
            "Part", "Book", "Prologue", "Epilogue", "Its", "Our", "Your", "Their", "Well", "Oh", "Not", "All");
    private static final Pattern ANIMAL = Pattern.compile(
            "(?i)\\b(mole|rat|ratty|toad|badger|fox|rabbit|hare|mouse|dog|cat|bird|bear|wolf|pig|duck|owl|frog|otter|"
                    + "weasel|stoat|squirrel|hedgehog|horse|goat|sheep|cow|lion|tiger|monkey|elephant)\\b");
    private static final int MAX_CHARACTERS = 6;

    private static final Hat[] HATS = {Hat.NONE, Hat.CAP, Hat.NONE, Hat.TOP_HAT, Hat.NONE, Hat.STRAW_HAT};
    private static final Accessory[] ACCESSORIES = {Accessory.SCARF, Accessory.NONE, Accessory.GLASSES,
            Accessory.NONE, Accessory.BOW, Accessory.NONE};
    private static final Hair[] HAIR = {Hair.SHORT, Hair.LONG, Hair.CURLY, Hair.NONE, Hair.PONYTAIL, Hair.SPIKY};
    private static final Ears[] EARS = {Ears.ROUND, Ears.POINTY, Ears.LONG, Ears.FLOPPY};
    private static final AnimalTail[] TAILS = {AnimalTail.SHORT, AnimalTail.LONG, AnimalTail.BUSHY, AnimalTail.CURLY};

    private static final Map<Pattern, PlaceDto> PLACES = new LinkedHashMap<>();

    static {
        place("river|riverbank|bank|stream", "The river bank", "Grass beside the water.", Background.RIVERBANK);
        place("wood|woods|forest", "The wood", "Lots of trees.", Background.FOREST);
        place("burrow|tunnel|hole", "The burrow", "A home under the ground.", Background.BURROW);
        place("house|home|kitchen|room|attic|hall", "Home", "Inside a house.", Background.ROOM);
        place("garden", "The garden", "Grass and flowers.", Background.GARDEN);
        place("field|meadow|hill", "The field", "Open grass.", Background.FIELD);
        place("town|village|street|road", "The town", "Houses and a street.", Background.TOWN);
        place("castle|palace", "The castle", "A big stone castle.", Background.CASTLE);
        place("school|classroom", "School", "A classroom.", Background.SCHOOL);
        place("beach|sea|shore", "The beach", "Sand next to the sea.", Background.BEACH);
        place("boat|ship", "The boat", "A boat on the water.", Background.BOAT);
        place("cave", "The cave", "A dark cave.", Background.CAVE);
    }

    private static void place(String words, String name, String description, Background background) {
        PLACES.put(Pattern.compile("(?i)\\b(" + words + ")\\b"), new PlaceDto(name, description, background));
    }

    private final SheetRules rules;

    public DemoCharacterFinder(SheetRules rules) {
        this.rules = rules;
    }

    @Override
    public int calls(String storyText) {
        return 0;
    }

    @Override
    public CharacterSheet find(String storyText) {
        List<CharacterDesign> characters = new ArrayList<>();
        for (String name : names(storyText)) {
            int i = characters.size();
            String animalWord = ANIMAL.matcher(name).matches() ? name : nearbyAnimal(storyText, name);
            Species species = Species.fromWord(animalWord);
            boolean known = species != Species.NONE;
            characters.add(animalWord != null
                    ? new CharacterDesign(name, name + ": an animal in the story.", CharacterKind.ANIMAL, null,
                            HATS[i % HATS.length], Hair.NONE, ACCESSORIES[i % ACCESSORIES.length],
                            known ? null : EARS[i % EARS.length], known ? null : TAILS[i % TAILS.length],
                            known ? null : i % 2 == 0 ? Snout.SHORT : Snout.LONG, false, species, null)
                    : new CharacterDesign(name, name + ": a person in the story.", CharacterKind.PERSON, null,
                            HATS[i % HATS.length], HAIR[i % HAIR.length], ACCESSORIES[i % ACCESSORIES.length],
                            Ears.NONE, AnimalTail.NONE, Snout.NONE, false));
        }
        return rules.normalize(new CharacterSheet(characters, placesIn(storyText)));
    }

    /** The places whose words appear in the text, in a fixed order. */
    static List<PlaceDto> placesIn(String text) {
        List<PlaceDto> places = new ArrayList<>();
        PLACES.forEach((pattern, place) -> {
            if (pattern.matcher(text).find()) {
                places.add(place);
            }
        });
        return places;
    }

    /** Capitalized words used twice or more, or right before or after "said", in order of first use. */
    static List<String> names(String text) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        NAME.matcher(text).results().map(MatchResult::group).filter(n -> !NOT_NAMES.contains(n))
                .forEach(n -> counts.merge(n, 1, Integer::sum));
        List<String> names = new ArrayList<>();
        counts.forEach((name, count) -> {
            boolean speaks = Pattern.compile("\\b(said|asked|shouted|whispered|called|cried) " + name + "\\b|\\b"
                    + name + " (said|asked|shouted|whispered|called|cried)\\b").matcher(text).find();
            if ((count >= 2 || speaks) && names.size() < MAX_CHARACTERS) {
                names.add(name);
            }
        });
        if (names.isEmpty()) {
            counts.keySet().stream().limit(2).forEach(names::add);
        }
        return names.isEmpty() ? List.of("Hero") : names;
    }

    /** "the Mole", "Ratty the rat": the animal word right next to the name, or null when there is none. */
    private static String nearbyAnimal(String text, String name) {
        // Two separate searches: in "ran to Tom the cat", "to Tom" must not hide "Tom the cat".
        for (String regex : List.of("(?i)\\b(\\w+) " + Pattern.quote(name) + "\\b",
                "(?i)\\b" + Pattern.quote(name) + ",? the (\\w+)")) {
            Matcher m = Pattern.compile(regex).matcher(text);
            while (m.find()) {
                String word = m.group(1).toLowerCase(Locale.ROOT);
                if (ANIMAL.matcher(word).matches()) {
                    return word;
                }
            }
        }
        return null;
    }
}
