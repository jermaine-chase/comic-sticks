package com.comicsticks.service;

import com.comicsticks.dto.ChapterBookends;
import com.comicsticks.dto.ComicOptions;
import com.comicsticks.dto.ComicStoryResponse;

/**
 * Writes a chapter's opening "Who's who" and closing "What happened", and the optional questions.
 * Runs on the finished, normalized comic, so panel numbers are final.
 */
public interface BookendsWriter {

    ChapterBookends write(ComicStoryResponse story, ComicOptions options);
}
