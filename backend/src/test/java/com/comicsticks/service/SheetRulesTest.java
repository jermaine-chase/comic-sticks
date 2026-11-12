package com.comicsticks.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

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
import com.comicsticks.support.TestProperties;

class SheetRulesTest {

    private final SheetRules rules = new SheetRules(TestProperties.properties(12));

    private static CharacterDesign design(String name, CharacterKind kind, Hat hat, Hair hair, Accessory accessory,
            Ears ears) {
        return new CharacterDesign(name, " " + name + ":   someone. ", kind, null, hat, hair, accessory, ears,
                AnimalTail.LONG, Snout.LONG, false);
    }

    private CharacterDesign only(CharacterDesign design) {
        return rules.normalize(new CharacterSheet(List.of(design), List.of())).characters().getFirst();
    }

    @Test
    void aPersonHasNoAnimalParts() {
        CharacterDesign mia = only(design("Mia", CharacterKind.PERSON, Hat.CAP, null, null, Ears.POINTY));

        assertThat(mia.ears()).isEqualTo(Ears.NONE);
        assertThat(mia.tail()).isEqualTo(AnimalTail.NONE);
        assertThat(mia.snout()).isEqualTo(Snout.NONE);
        assertThat(mia.description()).isEqualTo("Mia: someone.");
    }

    @Test
    void anAnimalAlwaysLooksLikeAnAnimal() {
        var plain = new CharacterDesign("Mole", "", CharacterKind.ANIMAL, null, null, null, null, Ears.NONE,
                AnimalTail.NONE, Snout.NONE, false);

        CharacterDesign mole = only(plain);

        assertThat(mole.ears()).isEqualTo(Ears.ROUND);
        assertThat(mole.snout()).isEqualTo(Snout.SHORT);
    }

    @Test
    void keepsAtMostTwoIdentifyingFeatures() {
        CharacterDesign toad = only(design("Toad", CharacterKind.ANIMAL, Hat.TOP_HAT, Hair.CURLY, Accessory.CAPE,
                Ears.ROUND));

        assertThat(toad.accessory()).isEqualTo(Accessory.CAPE);
        assertThat(toad.hat()).isEqualTo(Hat.TOP_HAT);
        assertThat(toad.hair()).isEqualTo(Hair.NONE);
    }

    @Test
    void givesEachPersonWithoutAFeatureADifferentLookAndColour() {
        var people = IntStream.range(0, 3)
                .mapToObj(i -> design("P" + i, CharacterKind.PERSON, null, null, null, null))
                .toList();

        var designs = rules.normalize(new CharacterSheet(people, List.of())).characters();

        assertThat(designs).extracting(CharacterDesign::hair).doesNotHaveDuplicates().doesNotContain(Hair.NONE);
        assertThat(designs).extracting(CharacterDesign::color).doesNotHaveDuplicates();
    }

    @Test
    void dropsDuplicatesAndBlankNamesAndCapsTheSheet() {
        var many = IntStream.range(0, 20)
                .mapToObj(i -> design("C" + i, CharacterKind.PERSON, Hat.CAP, null, null, null))
                .toList();
        var withDuplicates = new java.util.ArrayList<>(many);
        withDuplicates.add(0, design("c3", CharacterKind.PERSON, null, null, null, null));
        withDuplicates.add(design(" ", CharacterKind.PERSON, null, null, null, null));
        var places = List.of(new PlaceDto("Home", "", null), new PlaceDto("home", "", Background.ROOM));

        CharacterSheet sheet = rules.normalize(new CharacterSheet(withDuplicates, places));

        assertThat(sheet.characters()).hasSize(12);
        assertThat(sheet.characters()).extracting(CharacterDesign::name).doesNotContain("C3");
        assertThat(sheet.places()).containsExactly(new PlaceDto("Home", "", Background.PLAIN));
    }

    @Test
    void findsACharacterByAShorterOrLongerFormOfTheirName() {
        var designs = rules.normalize(new CharacterSheet(List.of(
                design("Mr. Toad", CharacterKind.ANIMAL, null, null, null, null),
                design("Water Rat", CharacterKind.ANIMAL, null, null, null, null)), List.of())).characters();

        assertThat(SheetRules.find(designs, "toad")).map(CharacterDesign::name).hasValue("Mr. Toad");
        assertThat(SheetRules.find(designs, "the Water Rat")).map(CharacterDesign::name).hasValue("Water Rat");
        assertThat(SheetRules.find(designs, "Badger")).isEmpty();
    }
}
