package com.comicsticks.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("comic")
public record ComicProperties(Llm llm, Story story, Characters characters) {

    public record Llm(
            String baseUrl,
            String apiKey,
            String model,
            Duration timeout,
            int maxOutputTokens,
            double secondsPerPart,
            double secondsPerPanel,
            double secondsPerNotesCall,
            double secondsPerCharacterChunk) {
    }

    /**
     * The character pass, which builds the character sheet before any panel is drawn.
     *
     * @param chunkChars    the story is read in pieces of about this size, one LLM call each
     * @param maxCharacters most characters on a sheet
     * @param maxPlaces     most places on a sheet
     */
    public record Characters(int chunkChars, int maxCharacters, int maxPlaces) {
    }

    /**
     * @param maxInputChars            longest text accepted for one comic (one chapter)
     * @param maxPanels                most panels one comic may have; longer texts must be split
     * @param maxPartChars             most text sent to the LLM in one call, at any support level
     * @param level1MaxPartChars       at level 1, longer paragraphs are cut at sentences to about this length
     * @param shortLineChars           a paragraph shorter than this is a "short line" that may join a neighbour
     * @param level2ParagraphsPerPanel at level 2, paragraphs per panel (a lone leftover joins the group before)
     * @param level3MinParagraphs      at level 3 with no scene markers, the fewest paragraphs per panel
     * @param level3MaxParagraphs      at level 3, the most paragraphs per panel; longer scenes are split
     */
    public record Story(
            int maxInputChars,
            int maxPanels,
            int maxPartChars,
            int level1MaxPartChars,
            int shortLineChars,
            int level2ParagraphsPerPanel,
            int level3MinParagraphs,
            int level3MaxParagraphs) {
    }
}
