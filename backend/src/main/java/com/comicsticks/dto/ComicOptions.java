package com.comicsticks.dto;

/**
 * Per-comic settings sent with {@code /estimate} and {@code /generate}. When a toggle is off, the
 * LLM is not asked for that field at all, so it has less to do.
 *
 * @param supportLevel  how much text goes under each panel
 * @param feelingLabels whether each character gets a one-word feeling tag
 * @param notes         whether panels get "what it means" notes for idioms and other tricky phrases
 * @param questions     whether the chapter ends with simple comprehension questions
 * @param things        whether panels show the objects and background animals their text names
 */
public record ComicOptions(SupportLevel supportLevel, boolean feelingLabels, boolean notes, boolean questions,
        boolean things) {

    public static final ComicOptions DEFAULTS = new ComicOptions(SupportLevel.PARAGRAPH, true, true, false, true);

    /** Questions and things off. */
    public ComicOptions(SupportLevel supportLevel, boolean feelingLabels, boolean notes) {
        this(supportLevel, feelingLabels, notes, false, false);
    }

    /** Things off. */
    public ComicOptions(SupportLevel supportLevel, boolean feelingLabels, boolean notes, boolean questions) {
        this(supportLevel, feelingLabels, notes, questions, false);
    }

    /** Fills in the defaults for options a request left out: everything on except questions. */
    public static ComicOptions of(Integer supportLevel, Boolean feelingLabels, Boolean notes, Boolean questions,
            Boolean things) {
        return new ComicOptions(SupportLevel.of(supportLevel), feelingLabels == null || feelingLabels,
                notes == null || notes, Boolean.TRUE.equals(questions), things == null || things);
    }
}
