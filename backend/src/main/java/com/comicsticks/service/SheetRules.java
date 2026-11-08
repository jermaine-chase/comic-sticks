package com.comicsticks.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.comicsticks.config.ComicProperties;
import com.comicsticks.dto.Accessory;
import com.comicsticks.dto.AnimalTail;
import com.comicsticks.dto.Background;
import com.comicsticks.dto.CharacterDesign;
import com.comicsticks.dto.CharacterDto;
import com.comicsticks.dto.CharacterKind;
import com.comicsticks.dto.CharacterSheet;
import com.comicsticks.dto.ComicStoryResponse;
import com.comicsticks.dto.DialogueDto;
import com.comicsticks.dto.Ears;
import com.comicsticks.dto.Hair;
import com.comicsticks.dto.Hat;
import com.comicsticks.dto.PanelDto;
import com.comicsticks.dto.PlaceDto;
import com.comicsticks.dto.Snout;

/**
 * Keeps a character sheet usable and keeps panels faithful to it:
 *
 * <ul>
 * <li>names are unique, each character has a colour, a person has no animal parts, an animal has
 * ears or a snout, and at most two of hat, hair and accessory are set, so designs stay simple;
 * <li>panels use the sheet's spelling of each name and place, and a character a panel names who is
 * not on the sheet gets a plain default design, marked as added.
 * </ul>
 */
@Component
public class SheetRules {

    /** Distinct, dark enough to read on a light background. */
    static final List<String> PALETTE = List.of("#1b1b1f", "#c8321f", "#1f5fbf", "#1c8a4a", "#8a3fb8",
            "#b85f00", "#0f7f86", "#a0306e", "#5a4a2f", "#4a5d8f", "#6b8e23", "#8b0000");
    private static final Pattern HEX_COLOR = Pattern.compile("#[0-9a-fA-F]{6}");
    private static final int MAX_FEATURES = 2;
    private static final int MAX_DESCRIPTION_CHARS = 140;
    /** Simple looks handed out in turn when a person has no feature, so two people never look alike. */
    private static final Hair[] DEFAULT_HAIR = {Hair.SHORT, Hair.LONG, Hair.CURLY, Hair.SPIKY, Hair.PONYTAIL, Hair.BUN};

    private final int maxCharacters;
    private final int maxPlaces;

    public SheetRules(ComicProperties properties) {
        this.maxCharacters = properties.characters().maxCharacters();
        this.maxPlaces = properties.characters().maxPlaces();
    }

    public CharacterSheet normalize(CharacterSheet sheet) {
        if (sheet == null) {
            return CharacterSheet.EMPTY;
        }
        List<CharacterDesign> characters = new ArrayList<>();
        for (CharacterDesign design : listOf(sheet.characters())) {
            if (design == null || text(design.name()).isEmpty() || find(characters, design.name()).isPresent()
                    || characters.size() == maxCharacters) {
                continue;
            }
            characters.add(normalize(design, characters.size()));
        }
        List<PlaceDto> places = new ArrayList<>();
        for (PlaceDto place : listOf(sheet.places())) {
            if (place == null || text(place.name()).isEmpty() || findPlace(places, place.name()).isPresent()
                    || places.size() == maxPlaces) {
                continue;
            }
            places.add(new PlaceDto(text(place.name()), oneLine(place.description()),
                    place.background() == null ? Background.PLAIN : place.background()));
        }
        return new CharacterSheet(characters, places);
    }

    private static CharacterDesign normalize(CharacterDesign d, int index) {
        CharacterKind kind = d.kind() == null ? CharacterKind.PERSON : d.kind();
        Hat hat = or(d.hat(), Hat.NONE);
        Hair hair = or(d.hair(), Hair.NONE);
        Accessory accessory = or(d.accessory(), Accessory.NONE);
        Ears ears = or(d.ears(), Ears.NONE);
        AnimalTail tail = or(d.tail(), AnimalTail.NONE);
        Snout snout = or(d.snout(), Snout.NONE);

        if (kind == CharacterKind.PERSON) {
            ears = Ears.NONE;
            tail = AnimalTail.NONE;
            snout = Snout.NONE;
        } else if (ears == Ears.NONE && snout == Snout.NONE) {
            ears = Ears.ROUND;
            snout = Snout.SHORT;
        }
        // Keep the two most telling features: what is worn or carried, then a hat, then hair.
        int features = 0;
        if (accessory != Accessory.NONE) {
            features++;
        }
        if (hat != Hat.NONE && ++features > MAX_FEATURES) {
            hat = Hat.NONE;
        }
        if (hair != Hair.NONE && ++features > MAX_FEATURES) {
            hair = Hair.NONE;
        }
        if (kind == CharacterKind.PERSON && hat == Hat.NONE && hair == Hair.NONE && accessory == Accessory.NONE) {
            hair = DEFAULT_HAIR[index % DEFAULT_HAIR.length];
        }
        String color = d.color() != null && HEX_COLOR.matcher(d.color()).matches() ? d.color()
                : PALETTE.get(index % PALETTE.size());
        return new CharacterDesign(text(d.name()), oneLine(d.description()), kind, color, hat, hair, accessory,
                ears, tail, snout, d.added());
    }

    /** A plain design for a character a panel names who is not on the sheet. */
    public static CharacterDesign defaultDesign(String name, int index) {
        return normalize(new CharacterDesign(name, "", CharacterKind.PERSON, null, null, null, null, null, null, null,
                true), index);
    }

    /**
     * Makes every panel use the sheet's names and places, and returns the story with the sheet it
     * was drawn from. Characters not on the sheet are added to it with a default design.
     */
    public ComicStoryResponse reconcile(ComicStoryResponse story, CharacterSheet sheet) {
        CharacterSheet clean = normalize(sheet);
        List<CharacterDesign> designs = new ArrayList<>(clean.characters());
        List<PanelDto> panels = new ArrayList<>();
        for (PanelDto panel : listOf(story.panels())) {
            if (panel == null) {
                continue;
            }
            List<CharacterDto> characters = new ArrayList<>();
            for (CharacterDto c : listOf(panel.characters())) {
                if (c == null || text(c.name()).isEmpty()) {
                    continue;
                }
                String name = sheetName(designs, c.name());
                characters.add(new CharacterDto(name, c.pose(), c.expression(), c.facing(), c.feeling()));
            }
            List<DialogueDto> dialogue = listOf(panel.dialogue()).stream()
                    .filter(Objects::nonNull)
                    .map(d -> d.speaker() == null || d.speaker().equalsIgnoreCase("narrator") ? d
                            : new DialogueDto(sheetName(designs, d.speaker()), d.text(), d.type()))
                    .toList();
            String place = findPlace(clean.places(), panel.place()).map(PlaceDto::name).orElse("");
            panels.add(new PanelDto(panel.panelNumber(), panel.scene(), panel.caption(), characters, dialogue,
                    panel.sourceText(), panel.notes(), place));
        }
        return new ComicStoryResponse(story.title(), panels, designs, clean.places());
    }

    /** The sheet's spelling of {@code name}, adding a default design if the sheet lacks one. */
    private static String sheetName(List<CharacterDesign> designs, String name) {
        Optional<CharacterDesign> match = find(designs, name);
        if (match.isPresent()) {
            return match.get().name();
        }
        CharacterDesign added = defaultDesign(text(name), designs.size());
        designs.add(added);
        return added.name();
    }

    /**
     * Finds a design by name, ignoring case, or by a name that is one word of a longer one, so that
     * "Toad" and "Mr. Toad" are the same character.
     */
    static Optional<CharacterDesign> find(List<CharacterDesign> designs, String name) {
        String wanted = key(name);
        Optional<CharacterDesign> exact = designs.stream().filter(d -> key(d.name()).equals(wanted)).findFirst();
        if (exact.isPresent() || wanted.isEmpty()) {
            return exact;
        }
        List<CharacterDesign> partial = designs.stream()
                .filter(d -> containsWords(key(d.name()), wanted) || containsWords(wanted, key(d.name())))
                .toList();
        return partial.size() == 1 ? Optional.of(partial.getFirst()) : Optional.empty();
    }

    private static Optional<PlaceDto> findPlace(List<PlaceDto> places, String name) {
        String wanted = key(name);
        return wanted.isEmpty() ? Optional.empty()
                : places.stream().filter(p -> key(p.name()).equals(wanted)).findFirst();
    }

    private static String key(String name) {
        return text(name).toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N} ]", "").replaceAll("\\s+", " ");
    }

    /** Whether {@code inner} appears in {@code outer} as whole words, as "water rat" in "the water rat". */
    private static boolean containsWords(String outer, String inner) {
        return !inner.isEmpty() && (" " + outer + " ").contains(" " + inner + " ");
    }

    private static String oneLine(String text) {
        String line = text(text).replaceAll("\\s+", " ");
        return line.length() <= MAX_DESCRIPTION_CHARS ? line : line.substring(0, MAX_DESCRIPTION_CHARS).strip() + "...";
    }

    private static <T> T or(T value, T fallback) {
        return value == null ? fallback : value;
    }

    private static <T> List<T> listOf(List<T> list) {
        return list == null ? List.of() : list;
    }

    private static String text(String value) {
        return value == null ? "" : value.strip();
    }
}
