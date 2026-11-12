package com.comicsticks.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A saved comic: its title and size for the list, and the whole comic and its options as JSON. */
@Entity
@Table(name = "saved_comic")
public class SavedComic {

    @Id
    private UUID id;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(name = "panel_count", nullable = false)
    private int panelCount;

    @Column(name = "options_json", nullable = false, columnDefinition = "TEXT")
    private String optionsJson;

    @Column(name = "comic_json", nullable = false, columnDefinition = "TEXT")
    private String comicJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SavedComic() {
    }

    public SavedComic(UUID id, Instant createdAt) {
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    /** Replaces the saved content, as when the reader saves their edits. */
    public void update(String title, int panelCount, String optionsJson, String comicJson, Instant now) {
        this.title = title;
        this.panelCount = panelCount;
        this.optionsJson = optionsJson;
        this.comicJson = comicJson;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getPanelCount() {
        return panelCount;
    }

    public String getOptionsJson() {
        return optionsJson;
    }

    public String getComicJson() {
        return comicJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
