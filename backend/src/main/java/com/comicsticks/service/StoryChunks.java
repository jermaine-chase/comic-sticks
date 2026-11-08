package com.comicsticks.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Cuts a story into pieces of about a given size at paragraph breaks, for passes over the whole text. */
final class StoryChunks {

    private static final Pattern PARAGRAPH_BREAK = Pattern.compile("\n\s*\n");

    private StoryChunks() {
    }

    static List<String> split(String story, int chunkChars) {
        List<String> chunks = new ArrayList<>();
        StringBuilder chunk = new StringBuilder();
        for (String paragraph : PARAGRAPH_BREAK.split(StorySegmenter.normalizeLineEndings(story).strip())) {
            if (!chunk.isEmpty() && chunk.length() + paragraph.length() > chunkChars) {
                chunks.add(chunk.toString());
                chunk.setLength(0);
            }
            chunk.append(chunk.isEmpty() ? "" : "\n\n").append(paragraph.strip());
        }
        if (!chunk.isEmpty()) {
            chunks.add(chunk.toString());
        }
        return chunks;
    }
}
