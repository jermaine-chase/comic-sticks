package com.comicsticks.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.comicsticks.config.ComicProperties;
import com.comicsticks.service.StorySegmenter.Part;

class StorySegmenterTest {

    private static final int MAX_PANELS = 12;
    private static final int MAX_PART_CHARS = 300;
    private static final int MIN_PART_CHARS = 30;

    private final StorySegmenter segmenter = new StorySegmenter(new ComicProperties(
            new ComicProperties.Llm("u", "k", "m", Duration.ofSeconds(1), 1, 1, 1),
            new ComicProperties.Story(100_000, MAX_PANELS, MAX_PART_CHARS, MIN_PART_CHARS, 80)));

    private static String sentences(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> "This is sentence number " + i + " of the story.")
                .collect(Collectors.joining(" "));
    }

    private static int totalPanels(List<Part> parts) {
        return parts.stream().mapToInt(Part::panels).sum();
    }

    @Test
    void makesOnePartPerParagraph() {
        String first = "Mia found a dusty old map up in the attic.";
        String second = "Leo followed her up the ladder, frowning.";

        List<Part> parts = segmenter.split(first + "\n\n" + second);

        assertThat(parts).extracting(Part::text).containsExactly(first, second);
    }

    @Test
    void handlesWindowsLineEndings() {
        List<Part> parts = segmenter.split("Mia found a dusty old map up in the attic.\r\n\r\n"
                + "Leo followed her up the ladder, frowning.");

        assertThat(parts).hasSize(2);
    }

    @Test
    void treatsLinesAsParagraphsWhenThereAreNoBlankLines() {
        List<Part> parts = segmenter.split("Mia found a dusty old map up in the attic.\n"
                + "Leo followed her up the ladder, frowning.");

        assertThat(parts).hasSize(2);
    }

    @Test
    void keepsLineBreaksInsideAParagraphTogether() {
        List<Part> parts = segmenter.split("Mia found a dusty\nold map in the attic.\n\n"
                + "Leo followed her\nup the ladder, frowning.");

        assertThat(parts).extracting(Part::text).containsExactly(
                "Mia found a dusty old map in the attic.", "Leo followed her up the ladder, frowning.");
    }

    @Test
    void cutsAnOversizedParagraphAtSentenceBoundaries() {
        String paragraph = sentences(30);

        List<Part> parts = segmenter.split(paragraph);

        assertThat(parts.size()).isGreaterThan(1);
        assertThat(parts).allSatisfy(part -> {
            assertThat(part.text().length()).isLessThanOrEqualTo(MAX_PART_CHARS);
            assertThat(part.text()).endsWith(".");
        });
        assertThat(parts.stream().map(Part::text).collect(Collectors.joining(" "))).isEqualTo(paragraph);
    }

    @Test
    void joinsAHeadingToTheParagraphAfterIt() {
        List<Part> parts = segmenter.split("Chapter 1\n\nMia found a dusty old map up in the attic.");

        assertThat(parts).extracting(Part::text)
                .containsExactly("Chapter 1 Mia found a dusty old map up in the attic.");
    }

    @Test
    void joinsAShortFinalParagraphToThePreviousPart() {
        List<Part> parts = segmenter.split("Mia found a dusty old map up in the attic.\n\nThe end.");

        assertThat(parts).extracting(Part::text)
                .containsExactly("Mia found a dusty old map up in the attic. The end.");
    }

    @Test
    void keepsAShortClosingParagraphSeparateBecauseItCarriesTheEnding() {
        String ending = "Grandpa smiled at them.";

        List<Part> parts = segmenter.split("Mia found a dusty old map up in the attic.\n\n" + ending);

        assertThat(ending.length()).isLessThan(MIN_PART_CHARS);
        assertThat(parts).hasSize(2);
        assertThat(parts.getLast().text()).isEqualTo(ending);
    }

    @Test
    void neverMakesMorePartsThanPanelsAndKeepsAllTheText() {
        String story = IntStream.range(0, 40)
                .mapToObj(i -> "Paragraph " + i + " has enough words to stand on its own.")
                .collect(Collectors.joining("\n\n"));

        List<Part> parts = segmenter.split(story);

        assertThat(parts).hasSizeLessThanOrEqualTo(MAX_PANELS);
        String joined = parts.stream().map(Part::text).collect(Collectors.joining(" "));
        assertThat(joined).startsWith("Paragraph 0 ").endsWith("Paragraph 39 has enough words to stand on its own.");
    }

    @Test
    void keepsTheFinalParagraphSeparateWhenMerging() {
        String ending = "The dragon gave Mia the crown of stars.";
        String story = IntStream.range(0, 40)
                .mapToObj(i -> "Paragraph " + i + " has enough words to stand on its own and then some more.")
                .collect(Collectors.joining("\n\n")) + "\n\n" + ending;

        assertThat(segmenter.split(story).getLast().text()).isEqualTo(ending);
    }

    @Test
    void givesTheFinalPartTwoPanelsWhenOneIsSpare() {
        List<Part> parts = segmenter.split(sentences(40) + "\n\nThe dragon gave Mia the crown of stars.");

        assertThat(parts.size()).isLessThan(MAX_PANELS);
        assertThat(parts.getLast().panels()).isEqualTo(2);
        assertThat(totalPanels(parts)).isEqualTo(MAX_PANELS);
    }

    @Test
    void everyPartGetsAPanelAndTheTotalStaysInBounds() {
        String story = sentences(200) + "\n\nA short closing paragraph that ends it all.";

        List<Part> parts = segmenter.split(story);

        assertThat(parts).allSatisfy(part -> assertThat(part.panels()).isPositive());
        assertThat(totalPanels(parts)).isLessThanOrEqualTo(MAX_PANELS);
        assertThat(parts.getLast().text()).endsWith("ends it all.");
    }

    @Test
    void sharesPanelsInProportionToLength() {
        List<Part> parts = segmenter.split(sentences(6) + "\n\n" + sentences(2) + "\n\n" + sentences(2));

        assertThat(parts.getFirst().panels()).isGreaterThan(parts.get(1).panels());
    }

    @Test
    void shortStoryIsOnePartWithPanelsScaledToItsLength() {
        List<Part> parts = segmenter.split("x".repeat(160));

        assertThat(parts).hasSize(1);
        assertThat(parts.getFirst().panels()).isEqualTo(2);
    }
}
