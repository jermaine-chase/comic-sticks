package com.comicsticks.dto;

/**
 * Per-comic settings sent with {@code /estimate} and {@code /generate}.
 *
 * @param supportLevel how much text goes under each panel
 */
public record ComicOptions(SupportLevel supportLevel) {

    public static final ComicOptions DEFAULTS = new ComicOptions(SupportLevel.PARAGRAPH);

    public static ComicOptions of(Integer supportLevel) {
        return new ComicOptions(SupportLevel.of(supportLevel));
    }
}
