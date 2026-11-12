package com.comicsticks.dto;

/**
 * Per-comic settings sent with {@code /estimate} and {@code /generate}. When a toggle is off, the
 * LLM is not asked for that field at all, so it has less to do.
 *
 * @param supportLevel  how much text goes under each panel
 * @param feelingLabels whether each character gets a one-word feeling tag
 * @param notes         whether panels get "what it means" notes for idioms and other tricky phrases
 * @param questions     whether the chapter ends with simple comprehension questions
 */
public record ComicOptions(SupportLevel supportLevel, boolean feelingLabels, boolean notes, boolean questions) {

    public static final ComicOptions DEFAULTS = new ComicOptions(SupportLevel.PARAGRAPH, true, true, false);

    /** Questions off, as by default. */
    public ComicOptions(SupportLevel supportLevel, boolean feelingLabels, boolean notes) {
        this(supportLevel, feelingLabels, notes, false);
    }

    public static ComicOptions of(Integer supportLevel, Boolean feelingLabels, Boolean notes, Boolean questions) {
        return new ComicOptions(SupportLevel.of(supportLevel),
                feelingLabels == null || feelingLabels, notes == null || notes, Boolean.TRUE.equals(questions));
    }
}
