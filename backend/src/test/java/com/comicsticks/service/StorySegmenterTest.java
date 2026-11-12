package com.comicsticks.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;

import com.comicsticks.dto.SupportLevel;
import com.comicsticks.exception.ComicGenerationException;
import com.comicsticks.service.StorySegmenter.Part;
import com.comicsticks.support.TestProperties;

class StorySegmenterTest {

    private static final int MAX_PANELS = 12;
    private static final int LEVEL1_MAX_PART_CHARS = 300;

    private final StorySegmenter segmenter = new StorySegmenter(TestProperties.properties(MAX_PANELS));

    /** A paragraph long enough not to count as a short line. */
    private static String paragraph(int i) {
        return "Paragraph " + i + " tells what happened next, with enough words to stand alone.";
    }

    private static String paragraphs(int from, int to) {
        return IntStream.range(from, to).mapToObj(StorySegmenterTest::paragraph).collect(Collectors.joining("\n\n"));
    }

    private static String sentences(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> "This is sentence number " + i + " of the story.")
                .collect(Collectors.joining(" "));
    }

    /** The story's words in order, ignoring whitespace and wordless scene markers. */
    static String words(String text) {
        return Arrays.stream(text.strip().split("\\s+"))
                .filter(word -> word.codePoints().anyMatch(Character::isLetterOrDigit))
                .collect(Collectors.joining(" "));
    }

    private static List<String> sourceTexts(List<Part> parts) {
        return parts.stream().map(Part::sourceText).toList();
    }

    /** How many of the {@link #paragraph} paragraphs each part holds. */
    private static List<Integer> paragraphCounts(List<Part> parts) {
        return parts.stream().map(p -> p.text().split("Paragraph ", -1).length - 1).toList();
    }

    @ParameterizedTest
    @EnumSource(SupportLevel.class)
    void sourceTextJoinedTogetherReproducesTheOriginalTextAtEveryLevel(SupportLevel level) {
        String story = "Chapter 1\r\n\r\nMia found a dusty\nold map.  \"Look!\" she said.\r\n\r\n"
                + paragraphs(0, 9) + "\n\n* * *\n\n" + sentences(40) + "\n\n\n\n"
                + "“Ready?” asked Mia.\n\n“Yes!” said Leo.\n\nChapter Two\n\n"
                + paragraphs(9, 20) + "\n\n   The end.  ";

        List<Part> parts = new StorySegmenter(TestProperties.properties(200)).split(story, level);

        String joined = String.join(" ", sourceTexts(parts));
        assertThat(words(joined)).isEqualTo(words(story));
        assertThat(sourceTexts(parts)).noneMatch(s -> s.contains("* * *"));
    }

    @Test
    void sourceTextKeepsTheStorysOwnLineBreaksAndSpacing() {
        String first = "Mia found a dusty\nold map.  “Look!” she said, holding it up to the light.";

        List<Part> parts = segmenter.split(first + "\r\n\r\nLeo followed her up the ladder, frowning at the dust.");

        assertThat(parts.getFirst().sourceText()).isEqualTo(first);
        assertThat(parts.getFirst().text())
                .isEqualTo("Mia found a dusty old map. “Look!” she said, holding it up to the light.");
    }

    @Test
    void supportLevelMustBeOneTwoOrThree() {
        assertThat(SupportLevel.of(null)).isEqualTo(SupportLevel.PARAGRAPH);
        assertThat(SupportLevel.of(3)).isEqualTo(SupportLevel.SCENE);
        assertThatThrownBy(() -> SupportLevel.of(4))
                .isInstanceOfSatisfying(ComicGenerationException.class,
                        e -> assertThat(e.status()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Nested
    class Level1 {

        private List<Part> split(String story) {
            return segmenter.split(story, SupportLevel.PARAGRAPH);
        }

        @Test
        void makesOnePanelPerParagraph() {
            List<Part> parts = split(paragraphs(0, 5));

            assertThat(parts).hasSize(5);
            assertThat(parts).extracting(Part::text).containsExactly(
                    paragraph(0), paragraph(1), paragraph(2), paragraph(3), paragraph(4));
            assertThat(parts).allSatisfy(p -> assertThat(p.panels()).isEqualTo(1));
        }

        @Test
        void handlesWindowsLineEndingsAndLinesWithoutBlankLines() {
            assertThat(split(paragraph(0) + "\r\n\r\n" + paragraph(1))).hasSize(2);
            assertThat(split(paragraph(0) + "\n" + paragraph(1))).hasSize(2);
        }

        @Test
        void keepsLineBreaksInsideAParagraphTogether() {
            List<Part> parts = split("Mia found a dusty\nold map in the attic.\n\nLeo followed her\nup the ladder.");

            assertThat(parts).extracting(Part::text).containsExactly(
                    "Mia found a dusty old map in the attic.", "Leo followed her up the ladder.");
        }

        @Test
        void cutsALongParagraphAtSentencesIntoPiecesOfTwoToThreeLines() {
            String paragraph = sentences(30);

            List<Part> parts = split(paragraph);

            assertThat(parts.size()).isGreaterThanOrEqualTo(4);
            assertThat(parts).allSatisfy(part -> {
                assertThat(part.text().length()).isLessThanOrEqualTo(LEVEL1_MAX_PART_CHARS);
                assertThat(part.text()).endsWith(".");
            });
            assertThat(String.join(" ", sourceTexts(parts))).isEqualTo(paragraph);
        }

        @Test
        void joinsAHeadingToTheParagraphAfterIt() {
            List<Part> parts = split("Chapter 1\n\nThe River Bank\n\n" + paragraph(0) + "\n\n" + paragraph(1));

            assertThat(parts).hasSize(2);
            assertThat(parts.getFirst().sourceText()).isEqualTo("Chapter 1\n\nThe River Bank\n\n" + paragraph(0));
        }

        @Test
        void keepsAShortLineThatIsNotDialogueOnItsOwn() {
            List<Part> parts = split(paragraph(0) + "\n\nHe ran.\n\n" + paragraph(1));

            assertThat(parts).extracting(Part::text).containsExactly(paragraph(0), "He ran.", paragraph(1));
        }

        @Test
        void joinsAShortLineOfDialogueAndItsReply() {
            List<Part> parts = split(paragraph(0) + "\n\n“Ready?” asked Mia.\n\n“Yes!” said Leo.\n\n"
                    + "\"Go,\" said Mia.\n\n" + paragraph(1));

            assertThat(parts).extracting(Part::text).containsExactly(paragraph(0),
                    "“Ready?” asked Mia. “Yes!” said Leo.", "\"Go,\" said Mia.", paragraph(1));
        }

        @Test
        void neverJoinsLongDialogue() {
            String line = "\"I have been thinking about the map all night, and I am sure it leads to the old mill,\" said Mia.";

            assertThat(split(line + "\n\n\"Then we go,\" said Leo.")).hasSize(2);
        }

        @Test
        void refusesTextThatNeedsMorePanelsInsteadOfMergingParagraphs() {
            String story = paragraphs(0, MAX_PANELS + 1);

            assertThatThrownBy(() -> split(story))
                    .isInstanceOfSatisfying(ComicGenerationException.class, e -> {
                        assertThat(e.status()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
                        assertThat(e.getMessage()).contains("13 panels", "at most 12", "chapters");
                    });
        }

        @Test
        void givesAnEndingOfSeveralSentencesASecondPanel() {
            List<Part> parts = split(paragraph(0) + "\n\nThe dragon bowed. It gave Mia the crown of stars.");

            assertThat(parts.getLast().panels()).isEqualTo(2);
            assertThat(parts.getFirst().panels()).isEqualTo(1);
        }

        @Test
        void aOneSentenceEndingKeepsOnePanel() {
            assertThat(split(paragraph(0) + "\n\nThe dragon gave Mia the crown.").getLast().panels()).isEqualTo(1);
        }

        @Test
        void theEndingGetsNoSecondPanelWhenTheComicIsFull() {
            List<Part> parts = split(paragraphs(0, MAX_PANELS - 1) + "\n\nThe end came. Everyone cheered.");

            assertThat(parts).hasSize(MAX_PANELS);
            assertThat(parts.getLast().panels()).isEqualTo(1);
        }
    }

    @Nested
    class Level2 {

        private List<Part> split(String story) {
            return segmenter.split(story, SupportLevel.GROUP);
        }

        @Test
        void groupsThreeToFourParagraphsPerPanel() {
            assertThat(paragraphCounts(split(paragraphs(0, 6)))).containsExactly(3, 3);
            assertThat(paragraphCounts(split(paragraphs(0, 7)))).containsExactly(3, 4);
            assertThat(paragraphCounts(split(paragraphs(0, 4)))).containsExactly(4);
            assertThat(paragraphCounts(split(paragraphs(0, 2)))).containsExactly(2);
        }

        @Test
        void neverGroupsAcrossASceneBreak() {
            List<Part> parts = split(paragraphs(0, 3) + "\n\n***\n\n" + paragraphs(3, 6));

            assertThat(paragraphCounts(parts)).containsExactly(3, 3);
        }

        @Test
        void aHeadingIsNotCountedAsAParagraph() {
            List<Part> parts = split("Chapter 1\n\n" + paragraphs(0, 6));

            assertThat(paragraphCounts(parts)).containsExactly(3, 3);
            assertThat(parts.getFirst().sourceText()).startsWith("Chapter 1\n\n");
        }
    }

    @Nested
    class Level3 {

        private List<Part> split(String story) {
            return segmenter.split(story, SupportLevel.SCENE);
        }

        @Test
        void makesOnePanelPerSceneAtEachKindOfMarker() {
            String story = paragraphs(0, 3) + "\n\n***\n\n" + paragraphs(3, 5) + "\n\n* * *\n\n"
                    + paragraphs(5, 9) + "\n\n\n\n" + paragraphs(9, 10);

            List<Part> parts = split(story);

            assertThat(paragraphCounts(parts)).containsExactly(3, 2, 4, 1);
            assertThat(sourceTexts(parts)).noneMatch(s -> s.contains("*"));
        }

        @Test
        void startsANewSceneAtAChapterHeading() {
            List<Part> parts = split("Chapter 1\n\n" + paragraphs(0, 2) + "\n\n***\n\n" + paragraph(2)
                    + "\n\nChapter 2\n\n" + paragraphs(3, 5));

            assertThat(parts).hasSize(3);
            assertThat(parts.getLast().sourceText()).startsWith("Chapter 2\n\n" + paragraph(3));
        }

        @Test
        void groupsSixToTenParagraphsWhenThereAreNoMarkers() {
            assertThat(paragraphCounts(split(paragraphs(0, 16)))).containsExactly(8, 8);
            assertThat(paragraphCounts(split(paragraphs(0, 11)))).containsExactly(5, 6);
            assertThat(paragraphCounts(split(paragraphs(0, 7)))).containsExactly(7);
        }

        @Test
        void splitsASceneTooLongForOnePicture() {
            List<Part> parts = split(paragraphs(0, 2) + "\n\n***\n\n" + paragraphs(2, 26));

            assertThat(paragraphCounts(parts)).containsExactly(2, 8, 8, 8);
        }
    }
}
