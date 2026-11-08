package com.comicsticks.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("comic")
public record ComicProperties(Llm llm, Story story) {

    public record Llm(
            String baseUrl,
            String apiKey,
            String model,
            Duration timeout,
            int maxOutputTokens,
            double secondsPerPart,
            double secondsPerPanel) {
    }

    public record Story(
            int maxInputChars,
            int maxPanels,
            int maxPartChars,
            int minPartChars,
            int charsPerPanel) {
    }
}
