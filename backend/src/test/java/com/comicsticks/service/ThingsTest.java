package com.comicsticks.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.comicsticks.dto.CharacterSheet;
import com.comicsticks.dto.ComicOptions;
import com.comicsticks.dto.ComicStoryResponse;
import com.comicsticks.dto.PanelDto;
import com.comicsticks.dto.SupportLevel;
import com.comicsticks.dto.Thing;
import com.comicsticks.dto.ThingDto;
import com.comicsticks.support.TestProperties;

/** Objects and background animals: only what the text names is drawn. */
class ThingsTest {

    private final PanelValidator validator = new PanelValidator();

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            Mia found a KEY.                     | KEY
            She dropped the keys.                | KEY
            Two boxes sat by the door.           | BOX
            Three mice ran past.                 | MOUSE
            The puppies slept.                   | DOG
            He opened the treasure  chest.       | BOX
            She drank a glass of milk.           | CUP
            """)
    void findsAThingByAnyOfItsWordsAndTheirPlurals(String text, Thing thing) {
        assertThat(ThingWords.names(text, thing)).isTrue();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            The monkey laughed.     | KEY
            He was boxing.          | BOX
            She hit her chest.      | BOX
            They watched the river. | CLOCK
            The keystone fell.      | KEY
            """)
    void needsAWholeWord(String text, Thing thing) {
        assertThat(ThingWords.names(text, thing)).isFalse();
    }

    @Test
    void everyThingHasLowercaseWords() {
        for (Thing thing : Thing.values()) {
            assertThat(thing.words()).isNotEmpty().allMatch(w -> w.equals(w.toLowerCase()));
        }
        assertThat(Arrays.stream(Thing.values()).filter(Thing::canBeHeld)).allMatch(t -> t.kind() == Thing.Kind.HELD);
    }

    @Test
    void listsThingsInTheOrderTheTextNamesThem() {
        assertThat(ThingWords.namedIn("A dog ran to the tree with a ball.")).containsExactly(Thing.DOG, Thing.TREE,
                Thing.BALL);
    }

    @Test
    void keepsOnlyThingsTheTextNamesOnceEachAndAtMostThree() {
        var things = validator.validThings(List.of(
                new ThingDto(Thing.MAP, "mia"),
                new ThingDto(Thing.SWORD, "Mia"),
                new ThingDto(Thing.MAP, ""),
                new ThingDto(Thing.TREE, "Mia"),
                new ThingDto(Thing.DOG, "Leo"),
                new ThingDto(Thing.CANDLE, "Nobody")), "Mia held a map under a tree. A dog and a candle.",
                List.of("Mia", "Leo"));

        assertThat(things).containsExactly(
                new ThingDto(Thing.MAP, "Mia"),
                new ThingDto(Thing.TREE, ""),
                new ThingDto(Thing.DOG, ""));
    }

    @Test
    void anAnimalCharacterIsNotAlsoABackgroundAnimal() {
        assertThat(validator.validThings(List.of(new ThingDto(Thing.DOG, "")), "Dog barked.", List.of("Dog")))
                .isEmpty();
    }

    @Test
    void theDemoDrawsNamedThingsInTheHandOfWhoeverHoldsThem() {
        assertThat(DemoComicGenerator.things("Mia found a map. A dog barked at the door.", List.of("Mia", "Leo")))
                .containsExactly(
                        new ThingDto(Thing.MAP, "Mia"),
                        new ThingDto(Thing.DOG, ""),
                        new ThingDto(Thing.DOOR, ""));
        assertThat(DemoComicGenerator.things("Leo saw a key. Mia carried the lamp.", List.of("Mia", "Leo")))
                .containsExactly(new ThingDto(Thing.KEY, ""), new ThingDto(Thing.LAMP, "Mia"));
    }

    @Test
    void theDemoFillsThingsThatPassValidationAndLeavesThemOutWhenOff() {
        String story = "Mia found a map in the attic.\n\nA dog barked. Mia picked up a stick.";
        var segmenter = new StorySegmenter(TestProperties.properties(200));
        var rules = new SheetRules(TestProperties.properties(200));
        var demo = new DemoComicGenerator(segmenter, new DemoCharacterFinder(rules), rules);

        ComicStoryResponse on = demo.generate(story, ComicOptions.DEFAULTS, CharacterSheet.EMPTY);
        for (PanelDto panel : on.panels()) {
            List<String> names = panel.characters().stream().map(c -> c.name()).toList();
            // A part's first panel can have no text of its own; its text is under the next panel.
            String text = panel.sourceText().isEmpty() ? story : panel.sourceText();
            assertThat(validator.validThings(panel.things(), text, names)).isEqualTo(panel.things());
        }
        assertThat(on.panels().stream().flatMap(p -> p.things().stream()).map(ThingDto::thing))
                .containsExactly(Thing.MAP, Thing.DOG, Thing.STICK);

        ComicStoryResponse off = demo.generate(story, new ComicOptions(SupportLevel.PARAGRAPH, true, true), CharacterSheet.EMPTY);
        assertThat(off.panels()).allMatch(p -> p.things().isEmpty());
    }

    @Test
    void theSchemaOffersEveryThingAndOnlyThePanelsCharactersAsHolders() {
        var schema = ComicSchema.things(3, List.of("Mia"));

        assertThat(schema.toString()).contains("maxItems=3", "enum=[Mia, ]", "butterfly");
    }
}
