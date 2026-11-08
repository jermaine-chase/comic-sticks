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
import com.comicsticks.dto.CharacterSheet;
import com.comicsticks.dto.ComicEstimateResponse;
import com.comicsticks.dto.ComicOptions;
import com.comicsticks.dto.ComicStoryResponse;
import com.comicsticks.dto.GenerateComicRequest;
import com.comicsticks.exception.ComicGenerationException;
import com.comicsticks.service.CharacterFinder;
import com.comicsticks.service.ComicGenerator;
import com.comicsticks.service.ComicStoryNormalizer;
import com.comicsticks.service.SheetRules;
import com.comicsticks.service.StoryTextExtractor;

import jakarta.validation.Valid;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * The single-piece API. Each endpoint takes story text as JSON or an uploaded .txt or .pdf file. A
 * multipart request sends the options as form fields, and the character sheet as a JSON string.
 */
@RestController
@RequestMapping("/api/comics")
public class ComicController {

    private final ComicGenerator generator;
    private final CharacterFinder finder;
    private final SheetRules sheetRules;
    private final ComicStoryNormalizer normalizer;
    private final StoryTextExtractor extractor;
    private final JsonMapper json = JsonMapper.builder().build();
    private final int maxInputChars;

    public ComicController(ComicGenerator generator, CharacterFinder finder, SheetRules sheetRules,
            ComicStoryNormalizer normalizer, StoryTextExtractor extractor, ComicProperties properties) {
        this.generator = generator;
        this.finder = finder;
        this.sheetRules = sheetRules;
        this.normalizer = normalizer;
        this.extractor = extractor;
        this.maxInputChars = properties.story().maxInputChars();
    }

    /** Predicts the run time for story text sent as JSON, without calling the LLM. */
    @PostMapping(path = "/estimate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ComicEstimateResponse estimateFromText(@Valid @RequestBody GenerateComicRequest request) {
        return generator.estimate(validated(request.text()), request.options(), hasSheet(request.characterSheet()));
    }

    /** Predicts the run time for an uploaded .txt or .pdf file, without calling the LLM. */
    @PostMapping(path = "/estimate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ComicEstimateResponse estimateFromFile(@RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Integer supportLevel,
            @RequestParam(required = false) Boolean feelingLabels, @RequestParam(required = false) Boolean notes,
            @RequestParam(required = false) String characterSheet) {
        return generator.estimate(validated(extractor.extract(file)),
                ComicOptions.of(supportLevel, feelingLabels, notes), hasSheet(sheet(characterSheet)));
    }

    /** Runs the character pass, so the sheet can be reviewed and edited before drawing. */
    @PostMapping(path = "/characters", consumes = MediaType.APPLICATION_JSON_VALUE)
    public CharacterSheet charactersFromText(@Valid @RequestBody GenerateComicRequest request) {
        return sheetRules.normalize(finder.find(validated(request.text())));
    }

    @PostMapping(path = "/characters", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CharacterSheet charactersFromFile(@RequestParam("file") MultipartFile file) {
        return sheetRules.normalize(finder.find(validated(extractor.extract(file))));
    }

    /** Generates a comic from raw story text sent as JSON. */
    @PostMapping(path = "/generate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ComicStoryResponse generateFromText(@Valid @RequestBody GenerateComicRequest request) {
        return generate(request.text(), request.options(), request.characterSheet());
    }

    /** Generates a comic from an uploaded .txt or .pdf file. */
    @PostMapping(path = "/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ComicStoryResponse generateFromFile(@RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Integer supportLevel,
            @RequestParam(required = false) Boolean feelingLabels, @RequestParam(required = false) Boolean notes,
            @RequestParam(required = false) String characterSheet) {
        return generate(extractor.extract(file), ComicOptions.of(supportLevel, feelingLabels, notes),
                sheet(characterSheet));
    }

    private ComicStoryResponse generate(String rawText, ComicOptions options, CharacterSheet sheet) {
        return normalizer.normalize(generator.generate(validated(rawText), options,
                sheet == null ? CharacterSheet.EMPTY : sheet));
    }

    private static boolean hasSheet(CharacterSheet sheet) {
        return sheet != null && !sheet.isEmpty();
    }

    private CharacterSheet sheet(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return null;
        }
        try {
            return json.readValue(rawJson, CharacterSheet.class);
        } catch (JacksonException e) {
            throw new ComicGenerationException(HttpStatus.BAD_REQUEST, "The character sheet could not be read.", e);
        }
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
