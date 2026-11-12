package com.comicsticks.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.comicsticks.dto.ComicStoryResponse;
import com.comicsticks.dto.SavedComicDtos.SaveComicRequest;
import com.comicsticks.dto.SavedComicDtos.SavedComicResponse;
import com.comicsticks.dto.SavedComicDtos.SavedComicSummary;
import com.comicsticks.dto.SavedComicDtos.SavedOptions;
import com.comicsticks.persistence.SavedComic;
import com.comicsticks.persistence.SavedComicRepository;

import tools.jackson.databind.json.JsonMapper;

/**
 * Saves comics as the reader last edited them, lists them and opens them again. The comic is kept
 * as JSON, so a reopened comic is exactly what was saved.
 */
@Service
public class SavedComicService {

    static final int MAX_TITLE_CHARS = 300;
    static final String UNTITLED = "Untitled comic";

    private final SavedComicRepository repository;
    private final JsonMapper json = JsonMapper.builder().build();
    private final Clock clock;
    private final int maxJsonChars;

    public SavedComicService(SavedComicRepository repository, Clock clock,
            @Value("${comic.saved.max-json-chars:5000000}") int maxJsonChars) {
        this.repository = repository;
        this.clock = clock;
        this.maxJsonChars = maxJsonChars;
    }

    @Transactional(readOnly = true)
    public List<SavedComicSummary> list() {
        return repository.findAllByOrderByUpdatedAtDesc().stream().map(SavedComicService::summary).toList();
    }

    @Transactional(readOnly = true)
    public SavedComicResponse get(UUID id) {
        SavedComic saved = find(id);
        return new SavedComicResponse(saved.getId(), saved.getTitle(), saved.getPanelCount(), saved.getCreatedAt(),
                saved.getUpdatedAt(), json.readValue(saved.getComicJson(), ComicStoryResponse.class),
                json.readValue(saved.getOptionsJson(), SavedOptions.class));
    }

    @Transactional
    public SavedComicSummary create(SaveComicRequest request) {
        SavedComic saved = new SavedComic(UUID.randomUUID(), clock.instant());
        fill(saved, request, saved.getCreatedAt());
        return summary(repository.save(saved));
    }

    @Transactional
    public SavedComicSummary update(UUID id, SaveComicRequest request) {
        SavedComic saved = find(id);
        fill(saved, request, laterThan(saved.getUpdatedAt()));
        return summary(repository.save(saved));
    }

    @Transactional
    public void delete(UUID id) {
        repository.delete(find(id));
    }

    private void fill(SavedComic saved, SaveComicRequest request, Instant now) {
        ComicStoryResponse comic = request.comic();
        String comicJson = json.writeValueAsString(comic);
        if (comicJson.length() > maxJsonChars) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE,
                    "This comic is too big to save. Try saving a shorter piece.");
        }
        SavedOptions options = request.options() == null ? new SavedOptions(null, null, null, null, null)
                : request.options();
        saved.update(title(comic.title()), comic.panels() == null ? 0 : comic.panels().size(),
                json.writeValueAsString(options), comicJson, now);
    }

    /** Never earlier than the last save, so "most recent first" holds even if the clock steps back. */
    private Instant laterThan(Instant previous) {
        Instant now = clock.instant();
        return now.isAfter(previous) ? now : previous.plusMillis(1);
    }

    private SavedComic find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "That comic was not found. It may have been deleted."));
    }

    static String title(String title) {
        String clean = title == null ? "" : title.strip().replaceAll("\\s+", " ");
        if (clean.isEmpty()) {
            return UNTITLED;
        }
        return clean.length() <= MAX_TITLE_CHARS ? clean : clean.substring(0, MAX_TITLE_CHARS).strip();
    }

    private static SavedComicSummary summary(SavedComic saved) {
        return new SavedComicSummary(saved.getId(), saved.getTitle(), saved.getPanelCount(), saved.getCreatedAt(),
                saved.getUpdatedAt());
    }
}
