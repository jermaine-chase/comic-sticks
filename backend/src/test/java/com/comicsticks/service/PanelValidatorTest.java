package com.comicsticks.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.comicsticks.dto.CharacterDto;
import com.comicsticks.dto.ComicOptions;
import com.comicsticks.dto.DialogueDto;
import com.comicsticks.dto.DialogueType;
import com.comicsticks.dto.Expression;
import com.comicsticks.dto.Facing;
import com.comicsticks.dto.NoteDto;
import com.comicsticks.dto.PanelDto;
import com.comicsticks.dto.Pose;
import com.comicsticks.dto.Scene;
import com.comicsticks.dto.SupportLevel;

class PanelValidatorTest {

    private final PanelValidator validator = new PanelValidator();

    private static DialogueDto line(DialogueType type, String text) {
        return new DialogueDto("Mia", text, type);
    }

    private static PanelDto panel(List<DialogueDto> dialogue, CharacterDto... characters) {
        return new PanelDto(1, Scene.PLAIN, "", List.of(characters), dialogue, "", List.of());
    }

    @ParameterizedTest(name = "\"{1}\" is spoken in: {0}")
    @CsvSource(delimiter = '|', quoteCharacter = '`', value = {
            "“Look at this!” said Mia.                 | Look at this!",
            "\"Look at this!\" said Mia.                          | “look at THIS!”",
            "“It’s mine,” said Mia.                | It's mine",
            "\"It's mine,\" said Mia.                             | It’s mine.",
            "\"Wait   for \t me!\" Mia called.                    | wait for me",
            "\"Come here. Now,\" said Mia.                        | Now",
            "'Hullo, Mole!' said the Water Rat.                   | Hullo, Mole!",
            "‘Hullo, Mole!’ said the Water Rat.         | 'hullo, mole'",
    })
    void acceptsTheTextsOwnSpokenWordsAfterNormalizingQuotesCaseAndWhitespace(String text, String bubble) {
        assertThat(TextMatch.isSpokenIn(text, bubble)).isTrue();
        assertThat(PanelValidator.isValid(line(DialogueType.SPEECH, bubble), text)).isTrue();
    }

    @ParameterizedTest(name = "\"{1}\" is not spoken in: {0}")
    @CsvSource(delimiter = '|', quoteCharacter = '`', value = {
            "\"Look at this!\" said Mia.            | Look at that!",
            "\"Look at this!\" said Mia.            | said Mia",
            "Mia looked at the map.                 | Look at this!",
            "\"Look!\" said Mia.                    | ...",
    })
    void rejectsInventedRewordedOrNarratedWords(String text, String bubble) {
        assertThat(PanelValidator.isValid(line(DialogueType.SHOUT, bubble), text)).isFalse();
        assertThat(PanelValidator.isValid(line(DialogueType.WHISPER, bubble), text)).isFalse();
    }

    @Test
    void thoughtsNeedNotBeInTheText() {
        assertThat(PanelValidator.isValid(line(DialogueType.THOUGHT, "I want to go home."), "Mia sighed.")).isTrue();
        assertThat(PanelValidator.isValid(line(DialogueType.THOUGHT, "  "), "Mia sighed.")).isFalse();
    }

    @Test
    void reportsEachProblemBubbleForTheRetryAndCleanRemovesThem() {
        String text = "\"Hello!\" said Mia.";
        var good = line(DialogueType.SPEECH, "Hello!");
        var bad = line(DialogueType.SPEECH, "Goodbye!");
        List<PanelDto> panels = List.of(panel(List.of(good, bad)));

        assertThat(validator.dialogueProblems(panels, text))
                .containsExactly("The speech bubble \"Goodbye!\" is not words spoken in the text.");
        assertThat(validator.clean(panels, text, ComicOptions.DEFAULTS).getFirst().dialogue()).containsExactly(good);
    }

    @Test
    void feelingIsOneWordAndSetsTheFaceOrIsClearedWhenOff() {
        var mia = new CharacterDto("Mia", Pose.STANDING, Expression.HAPPY, Facing.RIGHT, "a bit nervous");
        List<PanelDto> panels = List.of(panel(List.of(), mia));

        var on = validator.clean(panels, "x", ComicOptions.DEFAULTS).getFirst().characters().getFirst();
        var off = validator.clean(panels, "x", new ComicOptions(SupportLevel.PARAGRAPH, false, true))
                .getFirst().characters().getFirst();

        assertThat(on.feeling()).isEqualTo("nervous");
        assertThat(on.expression()).isEqualTo(Expression.WORRIED);
        assertThat(off.feeling()).isEmpty();
        assertThat(off.expression()).isEqualTo(Expression.HAPPY);
    }

    @Test
    void keepsOnlyNotesWhosePhraseIsInTheSourceText() {
        String source = "Toad’s heart sank. He was\nin a pickle, and it was raining cats and dogs.";
        List<NoteDto> notes = List.of(
                new NoteDto("Toad's heart sank", "He felt very disappointed. His heart did not move."),
                new NoteDto("IN A PICKLE", "He was in trouble."),
                new NoteDto("over the moon", "Very happy."),
                new NoteDto("in a pickle", "A duplicate."),
                new NoteDto("raining cats and dogs", "  "),
                new NoteDto("", "Empty phrase."));

        assertThat(validator.validNotes(notes, source)).extracting(NoteDto::phrase)
                .containsExactly("Toad's heart sank", "IN A PICKLE");
    }

    @Test
    void shortensAMeaningToTwoSentencesAndCapsTheNumberOfNotes() {
        String source = "one two three four";
        List<NoteDto> notes = List.of(new NoteDto("one", "First. Second! Third? Fourth."),
                new NoteDto("two", "A."), new NoteDto("three", "B."), new NoteDto("four", "C."));

        List<NoteDto> kept = validator.validNotes(notes, source);

        assertThat(kept).hasSize(3);
        assertThat(kept.getFirst().meaning()).isEqualTo("First. Second!");
    }
}
