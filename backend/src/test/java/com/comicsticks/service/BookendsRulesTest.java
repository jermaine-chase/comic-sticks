package com.comicsticks.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.comicsticks.dto.ChapterBookends.AnswerChoice;
import com.comicsticks.dto.ChapterBookends.Question;
import com.comicsticks.dto.ChapterBookends.RecapItem;
import com.comicsticks.dto.CharacterDto;
import com.comicsticks.dto.ComicStoryResponse;
import com.comicsticks.dto.Expression;
import com.comicsticks.dto.Facing;
import com.comicsticks.dto.PanelDto;
import com.comicsticks.dto.Pose;
import com.comicsticks.dto.Scene;

class BookendsRulesTest {

    private static CharacterDto figure(String name, String feeling) {
        return new CharacterDto(name, Pose.STANDING, Expression.NEUTRAL, Facing.RIGHT, feeling);
    }

    private static PanelDto panel(int number, String text, CharacterDto... characters) {
        return new PanelDto(number, Scene.OUTDOOR, "", List.of(characters), List.of(), text, List.of(),
                number == 1 ? "The river bank" : "");
    }

    /** Six panels: Mole in all, Rat in 2 to 4, Badger only in 5. */
    private static final ComicStoryResponse STORY = new ComicStoryResponse("T", List.of(
            panel(1, "Mole walked to the river. It was sunny.", figure("Mole", "happy")),
            panel(2, "Rat rowed over. He waved.", figure("Rat", ""), figure("Mole", "")),
            panel(3, "", figure("Mole", ""), figure("Rat", "")),
            panel(4, "They had a picnic together.", figure("Mole", "excited"), figure("Rat", "")),
            panel(5, "Badger came out of the wood.", figure("Badger", ""), figure("Mole", "scared")),
            panel(6, "Mole went home to bed.", figure("Mole", ""))));

    @Test
    void whosWhoListsEveryoneInOrderOfFirstAppearance() {
        var whosWho = BookendsRules.whosWho(STORY, "  ");

        assertThat(whosWho.characters()).containsExactly("Mole", "Rat", "Badger");
        assertThat(whosWho.whereWeAre()).isEqualTo("We are at the river bank.");
        assertThat(BookendsRules.whosWho(STORY, "We are by the river in the morning.").whereWeAre())
                .isEqualTo("We are by the river in the morning.");
    }

    @Test
    void recapKeepsLinesForRealTextPanelsInOrderAndEndsWithTheFeeling() {
        List<RecapItem> suggested = List.of(
                new RecapItem("they had a picnic", 4, null, null),
                new RecapItem("Rat rowed over to Mole.", 2, null, null),
                new RecapItem("A panel with no text", 3, null, null),
                new RecapItem("A panel that does not exist", 99, null, null),
                new RecapItem("  ", 5, null, null));

        List<RecapItem> recap = BookendsRules.recap(STORY, suggested, "Mole", "tired");

        assertThat(recap).extracting(RecapItem::sentence).containsExactly(
                "Rat rowed over to Mole.", "They had a picnic.", "At the end, Mole feels tired.");
        assertThat(recap).extracting(RecapItem::panelNumber).containsExactly(2, 4, null);
        assertThat(recap.getLast().character()).isEqualTo("Mole");
        assertThat(recap.getLast().expression()).isEqualTo(Expression.TIRED);
    }

    @Test
    void recapFillsUpFromTheTextWhenTooFewLinesAreUsableAndNeverExceedsFive() {
        List<RecapItem> none = BookendsRules.recap(STORY, List.of(), "Mole", "calm");
        List<RecapItem> many = BookendsRules.recap(STORY, IntStream.of(1, 2, 4, 5, 6)
                .mapToObj(n -> new RecapItem("Line " + n, n, null, null)).toList(), "Mole", "calm");

        assertThat(none).hasSize(BookendsRules.MIN_RECAP);
        assertThat(none.getFirst().sentence()).isEqualTo("Mole walked to the river.");
        assertThat(many).hasSize(BookendsRules.MAX_RECAP);
        assertThat(many.getLast().sentence()).isEqualTo("At the end, Mole feels calm.");
    }

    @Test
    void mainCharacterMustBeInTheChapterAndFeelingFallsBackToTheirLastOne() {
        assertThat(BookendsRules.mainCharacter(STORY, "rat")).isEqualTo("Rat");
        assertThat(BookendsRules.mainCharacter(STORY, "Toad")).isEqualTo("Mole");
        assertThat(BookendsRules.endingFeeling(STORY, "Mole", "Very proud!")).isEqualTo("proud");
        assertThat(BookendsRules.endingFeeling(STORY, "Mole", "")).isEqualTo("scared");
        assertThat(BookendsRules.endingFeeling(STORY, "Badger", null)).isEqualTo("calm");
    }

    @Test
    void sentencesAreShortAndPlain() {
        String long_ = String.join(" ", IntStream.range(0, 40).mapToObj(i -> "word").toList());

        assertThat(BookendsRules.sentence("  mole   ran home ")).isEqualTo("Mole ran home.");
        assertThat(BookendsRules.sentence(long_).split(" ")).hasSize(25);
        assertThat(BookendsRules.sentence(long_)).endsWith("...");
    }

    @Test
    void whoQuestionOffersTheAnswerAndOthersFromTheChapterInNameOrder() {
        Question q = BookendsRules.whoQuestion(STORY, "who rowed over", "Rat");

        assertThat(q.question()).isEqualTo("Who rowed over?");
        assertThat(q.choices()).extracting(AnswerChoice::character).containsExactly("Badger", "Mole", "Rat");
        assertThat(q.choices().get(q.answerIndex()).character()).isEqualTo("Rat");
        assertThat(BookendsRules.whoQuestion(STORY, "Who?", "Toad")).as("not in the chapter").isNull();
    }

    @Test
    void pictureQuestionOffersTheAnswerAndPanelsWellApartFromIt() {
        Question q = BookendsRules.pictureQuestion(STORY, "Which picture shows the picnic.", 4);

        assertThat(q.question()).isEqualTo("Which picture shows the picnic?");
        assertThat(q.choices()).extracting(AnswerChoice::panelNumber).doesNotHaveDuplicates().contains(4)
                .doesNotContain(3).isSorted();
        assertThat(q.choices().get(q.answerIndex()).panelNumber()).isEqualTo(4);
        assertThat(BookendsRules.pictureQuestion(STORY, "Which?", 3)).as("a panel without text").isNull();
    }

    @Test
    void summaryNumbersTextPanelsAndSamplesWhenTooLong() {
        String all = BookendsRules.panelSummary(STORY, 10_000, 160);
        String few = BookendsRules.panelSummary(STORY, 2 * 40, 20);

        assertThat(all).contains("Panel 1: Mole walked to the river.", "Panel 6: Mole went home to bed.")
                .doesNotContain("Panel 3:");
        assertThat(few.lines()).hasSize(2);
        assertThat(few).contains("Panel 1:", "Panel 6:");
    }
}
