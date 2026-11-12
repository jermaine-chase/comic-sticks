package com.comicsticks.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.comicsticks.dto.AnimalTail;
import com.comicsticks.dto.CharacterDesign;
import com.comicsticks.dto.CharacterKind;
import com.comicsticks.dto.CharacterSheet;
import com.comicsticks.dto.Ears;
import com.comicsticks.dto.Hat;
import com.comicsticks.dto.Snout;
import com.comicsticks.dto.Species;
import com.comicsticks.dto.Stance;
import com.comicsticks.dto.Thing;
import com.comicsticks.dto.ThingDto;
import com.comicsticks.support.TestProperties;

import tools.jackson.databind.json.JsonMapper;

/** Animal species: each brings its own look and stance, and people never have one. */
class SpeciesTest {

    private final SheetRules rules = new SheetRules(TestProperties.properties(200));

    private CharacterDesign normalized(CharacterDesign design) {
        return rules.normalize(new CharacterSheet(List.of(design), List.of())).characters().getFirst();
    }

    private static CharacterDesign animal(Species species, Ears ears, Stance stance) {
        return new CharacterDesign("Rex", "", CharacterKind.ANIMAL, null, null, null, null, ears, null, null, false,
                species, stance);
    }

    @Test
    void aSpeciesFillsInTheAnimalPartsAndStanceLeftOut() {
        CharacterDesign dog = normalized(animal(Species.DOG, null, null));

        assertThat(dog.ears()).isEqualTo(Ears.FLOPPY);
        assertThat(dog.tail()).isEqualTo(AnimalTail.SHORT);
        assertThat(dog.snout()).isEqualTo(Snout.SHORT);
        assertThat(dog.stance()).isEqualTo(Stance.FOUR_LEGS);
    }

    @Test
    void keepsPartsAndStanceTheSheetChose() {
        CharacterDesign dog = normalized(animal(Species.DOG, Ears.POINTY, Stance.UPRIGHT));

        assertThat(dog.ears()).isEqualTo(Ears.POINTY);
        assertThat(dog.stance()).isEqualTo(Stance.UPRIGHT);
    }

    @Test
    void aFrogNeedsNoEarsOrSnout() {
        CharacterDesign frog = normalized(animal(Species.FROG, null, null));

        assertThat(frog.ears()).isEqualTo(Ears.NONE);
        assertThat(frog.snout()).isEqualTo(Snout.NONE);
    }

    @Test
    void anAnimalOfNoKnownSpeciesStillGetsEarsAndASnoutAndStandsUp() {
        CharacterDesign dragon = normalized(animal(null, null, null));

        assertThat(dragon.species()).isEqualTo(Species.NONE);
        assertThat(dragon.ears()).isEqualTo(Ears.ROUND);
        assertThat(dragon.snout()).isEqualTo(Snout.SHORT);
        assertThat(dragon.stance()).isEqualTo(Stance.UPRIGHT);
    }

    @Test
    void aPersonHasNoSpeciesAndAlwaysStandsUp() {
        CharacterDesign mia = normalized(new CharacterDesign("Mia", "", CharacterKind.PERSON, null, Hat.CAP, null,
                null, Ears.LONG, null, null, false, Species.CAT, Stance.FOUR_LEGS));

        assertThat(mia.species()).isEqualTo(Species.NONE);
        assertThat(mia.stance()).isEqualTo(Stance.UPRIGHT);
        assertThat(mia.ears()).isEqualTo(Ears.NONE);
    }

    @Test
    void theCharacterPassDrawsAKnownSpeciesWithItsOwnParts() {
        var found = new LlmCharacterFinder.FoundCharacter("Tom", "Tom: a cat.", CharacterKind.ANIMAL, Species.CAT,
                Stance.UPRIGHT, Hat.NONE, null, null, Ears.LONG, AnimalTail.CURLY, Snout.BEAK);
        CharacterDesign tom = normalized(LlmCharacterFinder.design(found));

        assertThat(tom.ears()).isEqualTo(Ears.POINTY);
        assertThat(tom.snout()).isEqualTo(Snout.SHORT);
        assertThat(tom.stance()).as("the model's stance is kept").isEqualTo(Stance.UPRIGHT);
        assertThat(LlmCharacterFinder.schema().toString()).contains("species", "four_legs", "hedgehog");
    }

    @Test
    void theDemoFindsTheSpeciesFromTheWordNextToTheName() {
        var sheet = new DemoCharacterFinder(rules).find("Rex the dog ran. Rex barked. Mia laughed. Mia sat.");

        CharacterDesign rex = sheet.characters().getFirst();
        assertThat(rex.species()).isEqualTo(Species.DOG);
        assertThat(rex.stance()).isEqualTo(Stance.FOUR_LEGS);
        assertThat(sheet.characters().get(1).species()).isEqualTo(Species.NONE);
    }

    @Test
    void theDemoFindsTheSpeciesAfterTheNameEvenWhenAWordComesBeforeIt() {
        var sheet = new DemoCharacterFinder(rules).find("Rex ran to Tom the cat. Tom sat. Rex sat.");

        assertThat(sheet.characters()).extracting(CharacterDesign::name).containsExactly("Rex", "Tom");
        assertThat(sheet.characters().get(1).species()).isEqualTo(Species.CAT);
    }

    @Test
    void namesASpeciesByItsWords() {
        assertThat(Species.fromWord("Kitten")).isEqualTo(Species.CAT);
        assertThat(Species.fromWord("ratty")).isEqualTo(Species.RAT);
        assertThat(Species.fromWord("hedgehog")).isEqualTo(Species.HEDGEHOG);
        assertThat(Species.fromWord("dragon")).isEqualTo(Species.NONE);
        assertThat(Species.fromWord(null)).isEqualTo(Species.NONE);
    }

    @Test
    void aBackgroundAnimalIsDroppedWhenAnAnimalCharacterIsThatSpecies() {
        var things = List.of(new ThingDto(Thing.DOG, ""), new ThingDto(Thing.CAT, ""), new ThingDto(Thing.KEY, ""));

        assertThat(PanelValidator.withoutCharacterAnimals(things, List.of(Species.DOG)))
                .extracting(ThingDto::thing).containsExactly(Thing.CAT, Thing.KEY);
    }

    @Test
    void readsAnOlderSheetWithoutSpeciesOrStance() {
        var design = JsonMapper.builder().build().readValue("""
                {"name":"Mole","kind":"animal","ears":"round"}""", CharacterDesign.class);

        assertThat(design.species()).isNull();
        assertThat(normalized(design).stance()).isEqualTo(Stance.UPRIGHT);
    }
}
