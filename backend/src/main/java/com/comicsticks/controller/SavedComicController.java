package com.comicsticks.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.comicsticks.dto.SavedComicDtos.SaveComicRequest;
import com.comicsticks.dto.SavedComicDtos.SavedComicResponse;
import com.comicsticks.dto.SavedComicDtos.SavedComicSummary;
import com.comicsticks.service.SavedComicService;

/**
 * Saved comics: save one, list them, open one, save edits to it and delete it. Ids are UUIDs; any
 * other id is rejected before it reaches the database.
 */
@RestController
@RequestMapping("/api/saved-comics")
public class SavedComicController {

    private final SavedComicService service;

    public SavedComicController(SavedComicService service) {
        this.service = service;
    }

    @GetMapping
    public List<SavedComicSummary> list() {
        return service.list();
    }

    @PostMapping
    public ResponseEntity<SavedComicSummary> create(@Valid @RequestBody SaveComicRequest request) {
        SavedComicSummary saved = service.create(request);
        return ResponseEntity.created(URI.create("/api/saved-comics/" + saved.id())).body(saved);
    }

    @GetMapping("/{id}")
    public SavedComicResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public SavedComicSummary update(@PathVariable UUID id, @Valid @RequestBody SaveComicRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
