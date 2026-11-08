package com.comicsticks.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.comicsticks.config.ComicProperties;
import com.comicsticks.dto.ComicEstimateResponse;
import com.comicsticks.dto.ComicStoryResponse;
import com.comicsticks.dto.GenerateComicRequest;
import com.comicsticks.exception.ComicGenerationException;
import com.comicsticks.service.ComicGenerator;
import com.comicsticks.service.ComicStoryNormalizer;
import com.comicsticks.service.StoryTextExtractor;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/comics")
public class ComicController {

    private final ComicGenerator generator;
    private final ComicStoryNormalizer normalizer;
    private final StoryTextExtractor extractor;
    private final int maxInputChars;

    public ComicController(ComicGenerator generator, ComicStoryNormalizer normalizer,
            StoryTextExtractor extractor, ComicProperties properties) {
        this.generator = generator;
        this.normalizer = normalizer;
        this.extractor = extractor;
        this.maxInputChars = properties.story().maxInputChars();
    }

    /** Predicts the run time for story text sent as JSON, without calling the LLM. */
    @PostMapping(path = "/estimate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ComicEstimateResponse estimateFromText(@Valid @RequestBody GenerateComicRequest request) {
        return generator.estimate(validated(request.text()));
    }

    /** Predicts the run time for an uploaded .txt or .pdf file, without calling the LLM. */
    @PostMapping(path = "/estimate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ComicEstimateResponse estimateFromFile(@RequestParam("file") MultipartFile file) {
        return generator.estimate(validated(extractor.extract(file)));
    }

    /** Generates a comic from raw story text sent as JSON. */
    @PostMapping(path = "/generate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ComicStoryResponse generateFromText(@Valid @RequestBody GenerateComicRequest request) {
        return generate(request.text());
    }

    /** Generates a comic from an uploaded .txt or .pdf file. */
    @PostMapping(path = "/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ComicStoryResponse generateFromFile(@RequestParam("file") MultipartFile file) {
        return generate(extractor.extract(file));
    }

    private ComicStoryResponse generate(String rawText) {
        return normalizer.normalize(generator.generate(validated(rawText)));
    }

    private String validated(String rawText) {
        String text = rawText.strip();
        if (text.isEmpty()) {
            throw new ComicGenerationException(HttpStatus.BAD_REQUEST, "Story text is required.");
        }
        if (text.length() > maxInputChars) {
            throw new ComicGenerationException(HttpStatus.CONTENT_TOO_LARGE,
                    "That story has %,d characters. The limit is %,d.".formatted(text.length(), maxInputChars));
        }
        return text;
    }
}
