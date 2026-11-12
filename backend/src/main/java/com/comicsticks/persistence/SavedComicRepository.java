package com.comicsticks.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedComicRepository extends JpaRepository<SavedComic, UUID> {

    /** Most recently saved first. */
    List<SavedComic> findAllByOrderByUpdatedAtDesc();
}
