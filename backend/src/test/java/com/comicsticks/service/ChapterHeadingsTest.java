package com.comicsticks.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ChapterHeadingsTest {

    @ParameterizedTest
    @ValueSource(strings = {"Chapter 1", "CHAPTER IV", "Chapter 4.", "Chapter Twenty-One", "chapter 3: The Wild Wood",
            "Chapter 7 — Dulce Domum", "Part One", "BOOK II", "Prologue", "Epilogue: Home Again", "XII", "12."})
    void recognizesChapterHeadings(String line) {
        assertThat(ChapterHeadings.isChapterHeading(line)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Part one of the plan was simple.", "Chapter and verse, he said.", "Mole",
            "The chapter was long.", "Book of spells", "Chapter One. It was a cold and windy morning."})
    void leavesOrdinaryLinesAlone(String line) {
        assertThat(ChapterHeadings.isChapterHeading(line)).isFalse();
    }
}
