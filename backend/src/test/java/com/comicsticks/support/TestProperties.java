package com.comicsticks.support;

import java.time.Duration;

import com.comicsticks.config.ComicProperties;

/** Properties for unit tests, with the same story limits as application.properties unless overridden. */
public final class TestProperties {

    private TestProperties() {
    }

    /** Seconds: 2 per part, 3 per panel, 1 per notes call, 5 per character chunk. */
    public static ComicProperties.Llm llm(String baseUrl, String key) {
        return new ComicProperties.Llm(baseUrl, key, "test-model", Duration.ofSeconds(5), 1000, 2, 3, 1, 5);
    }

    public static ComicProperties.Story story(int maxPanels) {
        return new ComicProperties.Story(100_000, maxPanels, 4000, 300, 60, 3, 6, 10);
    }

    public static ComicProperties.Characters characters() {
        return new ComicProperties.Characters(4000, 12, 8);
    }

    public static ComicProperties properties(int maxPanels) {
        return properties(llm("https://llm.test/v1", "test-key"), story(maxPanels));
    }

    public static ComicProperties properties(ComicProperties.Llm llm, ComicProperties.Story story) {
        return new ComicProperties(llm, story, characters());
    }
}
