package com.comicsticks.dto;

import java.util.List;

/**
 * What opens and closes a chapter, so the reader knows who and where before starting, and can
 * check what happened after finishing.
 *
 * @param whosWho   the opening panel
 * @param recap     3 to 5 short sentences in story order. The last always says how the main
 *                  character feels at the end.
 * @param questions simple comprehension questions with picture answers; empty when they are off
 */
public record ChapterBookends(WhosWho whosWho, List<RecapItem> recap, List<Question> questions) {

    /**
     * @param characters the sheet names of everyone in the chapter, in order of first appearance
     * @param whereWeAre one plain line, such as "We are at the river bank, on a sunny morning."
     */
    public record WhosWho(List<String> characters, String whereWeAre) {
    }

    /**
     * One recap sentence and its thumbnail: a reused panel, or a small figure of a character.
     *
     * @param panelNumber the panel shown as the thumbnail, or null for a character figure
     * @param character   the character drawn as the thumbnail, or null for a panel
     * @param expression  the face for a character thumbnail
     */
    public record RecapItem(String sentence, Integer panelNumber, String character, Expression expression) {
    }

    /**
     * A question with 2 or 3 picture answers. The choices are built by the backend from the
     * chapter's own characters and panels, so every answer is something the reader has seen.
     *
     * @param answerIndex the position of the right choice in {@code choices}
     */
    public record Question(String question, List<AnswerChoice> choices, int answerIndex) {
    }

    /** A picture answer: a character (drawn as a figure) or a panel (drawn as a thumbnail). */
    public record AnswerChoice(String character, Integer panelNumber) {
    }
}
