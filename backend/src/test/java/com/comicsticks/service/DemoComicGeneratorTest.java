package com.comicsticks.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.comicsticks.dto.Background;
import com.comicsticks.dto.CharacterDesign;
import com.comicsticks.dto.CharacterDto;
import com.comicsticks.dto.CharacterKind;
import com.comicsticks.dto.CharacterSheet;
import com.comicsticks.dto.PlaceDto;
import com.comicsticks.dto.ComicOptions;
import com.comicsticks.dto.ComicStoryResponse;
import com.comicsticks.dto.DialogueDto;
import com.comicsticks.dto.DialogueType;
import com.comicsticks.dto.Expression;
import com.comicsticks.dto.PanelDto;
import com.comicsticks.dto.SupportLevel;
import com.comicsticks.support.TestProperties;

/** The demo profile must fill every field with valid values, following the same rules as the LLM. */
class DemoComicGeneratorTest {

    private static final String STORY = """
            Mole was afraid of the dark wood. "Is anyone there?" whispered Mole.

            Later that night, Ratty found him. "Mole! Over here!" shouted Ratty. Mole's heart sank \
            when he saw how far they had to walk.

            They walked home together and Mole felt proud.""";

    private final StorySegmenter segmenter = new StorySegmenter(TestProperties.properties(200));
    private final SheetRules rules = new SheetRules(TestProperties.properties(200));
    private final DemoCharacterFinder finder = new DemoCharacterFinder(rules);
    private final DemoComicGenerator demo = new DemoComicGenerator(segmenter, finder, rules);
    private final ComicStoryNormalizer normalizer = new ComicStoryNormalizer(TestProperties.properties(200));
    private final PanelValidator validator = new PanelValidator();

    private ComicStoryResponse generate(ComicOptions options) {
        return normalizer.normalize(demo.generate(STORY, options, CharacterSheet.EMPTY));
    }

    private static List<DialogueDto> dialogue(ComicStoryResponse story) {
        return story.panels().stream().flatMap(p -> p.dialogue().stream()).toList();
    }

    @Test
    void producesValidOutputForEveryField() {
        ComicStoryResponse story = generate(ComicOptions.DEFAULTS);

        String joined = String.join(" ", story.panels().stream().map(PanelDto::sourceText).toList());
        assertThat(StorySegmenterTest.words(joined)).isEqualTo(StorySegmenterTest.words(STORY));
        for (PanelDto panel : story.panels()) {
            String text = panel.sourceText();
            assertThat(validator.dialogueProblems(List.of(panel), text.isEmpty() ? STORY : text)).isEmpty();
            assertThat(validator.validNotes(panel.notes(), text)).isEqualTo(panel.notes());
            for (CharacterDto c : panel.characters()) {
                assertThat(c.feeling()).matches("[a-z]*");
                FeelingExpressions.expressionFor(c.feeling()).ifPresent(e -> assertThat(c.expression()).isEqualTo(e));
            }
        }
    }

    @Test
    void fillsBubblesFeelingsCaptionsAndNotesByRule() {
        ComicStoryResponse story = generate(ComicOptions.DEFAULTS);

        assertThat(dialogue(story)).extracting(DialogueDto::type)
                .contains(DialogueType.WHISPER, DialogueType.SHOUT);
        assertThat(dialogue(story).getFirst()).isEqualTo(new DialogueDto("Mole", "Is anyone there?", DialogueType.WHISPER));
        CharacterDto mole = story.panels().getFirst().characters().stream()
                .filter(c -> c.name().equals("Mole")).findFirst().orElseThrow();
        assertThat(mole.feeling()).isEqualTo("afraid");
        assertThat(mole.expression()).isEqualTo(Expression.SCARED);
        assertThat(story.panels()).extracting(PanelDto::caption).contains("Later that night...");
        assertThat(story.panels().stream().flatMap(p -> p.notes().stream()))
                .anySatisfy(n -> assertThat(n.phrase()).isEqualToIgnoringCase("Mole's heart sank"));
    }

    @Test
    void buildsACharacterSheetAndDrawsEveryPanelFromIt() {
        ComicStoryResponse story = generate(ComicOptions.DEFAULTS);

        assertThat(story.characters()).extracting(CharacterDesign::name).containsExactly("Mole", "Ratty");
        assertThat(story.characters()).allSatisfy(c -> {
            assertThat(c.kind()).isEqualTo(CharacterKind.ANIMAL);
            assertThat(c.color()).matches("#[0-9a-f]{6}");
            assertThat(c.added()).isFalse();
        });
        assertThat(story.places()).extracting(PlaceDto::background).contains(Background.FOREST);
        List<String> sheetNames = story.characters().stream().map(CharacterDesign::name).toList();
        List<String> placeNames = story.places().stream().map(PlaceDto::name).toList();
        assertThat(story.panels()).allSatisfy(p -> {
            assertThat(p.characters()).extracting(CharacterDto::name).allMatch(sheetNames::contains);
            assertThat(p.place().isEmpty() || placeNames.contains(p.place())).isTrue();
        });
        assertThat(story.panels().getFirst().place()).isEqualTo("The wood");
    }

    @Test
    void usesAReviewedSheetInsteadOfFindingOneAgain() {
        var reviewed = new CharacterSheet(
                List.of(new CharacterDesign("Mole", "Mole: shy.", CharacterKind.ANIMAL, "#123456", null, null, null,
                        null, null, null, false)),
                List.of());

        ComicStoryResponse story = normalizer.normalize(demo.generate(STORY, ComicOptions.DEFAULTS, reviewed));

        assertThat(story.characters().getFirst().color()).isEqualTo("#123456");
        assertThat(story.characters()).extracting(CharacterDesign::name).containsExactly("Mole");
    }

    @Test
    void togglesLeaveTheirFieldsEmpty() {
        ComicStoryResponse story = generate(new ComicOptions(SupportLevel.PARAGRAPH, false, false));

        assertThat(story.panels()).allSatisfy(p -> {
            assertThat(p.notes()).isEmpty();
            assertThat(p.characters()).allSatisfy(c -> assertThat(c.feeling()).isEmpty());
        });
    }
}
