package com.comicsticks.dto;

import java.util.List;

/** The full comic returned by {@code POST /api/comics/generate}. */
public record ComicStoryResponse(String title, List<PanelDto> panels) {
}
