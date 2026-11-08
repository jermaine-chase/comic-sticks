package com.comicsticks.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.comicsticks.config.ComicProperties;
import com.comicsticks.dto.CharacterDto;
import com.comicsticks.dto.ComicStoryResponse;
import com.comicsticks.dto.DialogueDto;
import com.comicsticks.dto.DialogueType;
import com.comicsticks.dto.Expression;
import com.comicsticks.dto.Facing;
import com.comicsticks.dto.PanelDto;
import com.comicsticks.dto.Pose;
import com.comicsticks.dto.Scene;
import com.comicsticks.exception.ComicGenerationException;

class ComicStoryNormalizerTest {

    private final ComicStoryNormalizer normalizer = new ComicStoryNormalizer(new ComicProperties(
            new ComicProperties.Llm("u", "k", "m", Duration.ofSeconds(1), 1, 1, 1),
            new ComicProperties.Story(1000, 3, 3000, 150, 80)));

    private static final CharacterDto MIA =
            new CharacterDto("Mia", Pose.WAVING, Expression.HAPPY, Facing.RIGHT);

    private static PanelDto panel(int number, List<CharacterDto> characters, List<DialogueDto> dialogue) {
        return new PanelDto(number, Scene.PLAIN, "", characters, dialogue);
    }

    @Test
    void renumbersPanelsAndCapsTheCount() {
        List<PanelDto> five = IntStream.range(0, 5).mapToObj(i -> panel(9, List.of(MIA), List.of())).toList();

        ComicStoryResponse story = normalizer.normalize(new ComicStoryResponse("T", five));

        assertThat(story.panels()).extracting(PanelDto::panelNumber).containsExactly(1, 2, 3);
    }

    @Test
    void addsAFigureForASpeakerMissingFromThePanel() {
        var line = new DialogueDto("Bob", "Hey", DialogueType.SPEECH);

        PanelDto result = normalizer.normalize(new ComicStoryResponse("T",
                List.of(panel(1, List.of(MIA), List.of(line))))).panels().getFirst();

        assertThat(result.characters()).extracting(CharacterDto::name).containsExactly("Mia", "Bob");
    }

    @Test
    void matchesSpeakerCaseInsensitivelyUsingThePanelSpelling() {
        var line = new DialogueDto("MIA", "Hey", DialogueType.SPEECH);

        PanelDto result = normalizer.normalize(new ComicStoryResponse("T",
                List.of(panel(1, List.of(MIA), List.of(line))))).panels().getFirst();

        assertThat(result.characters()).hasSize(1);
        assertThat(result.dialogue().getFirst().speaker()).isEqualTo("Mia");
    }

    @Test
    void movesNarratorLinesIntoTheCaption() {
        var line = new DialogueDto("Narrator", "Years passed.", DialogueType.SPEECH);
        var input = new PanelDto(1, Scene.PLAIN, "Later.", List.of(MIA), List.of(line));

        PanelDto result = normalizer.normalize(new ComicStoryResponse("T", List.of(input))).panels().getFirst();

        assertThat(result.dialogue()).isEmpty();
        assertThat(result.caption()).isEqualTo("Later. Years passed.");
    }

    @Test
    void fillsNullsWithDefaults() {
        var bare = new CharacterDto("Mia", null, null, null);
        var input = new PanelDto(0, null, null, Collections.singletonList(bare), null);

        ComicStoryResponse story = normalizer.normalize(new ComicStoryResponse(null, List.of(input)));

        PanelDto result = story.panels().getFirst();
        assertThat(story.title()).isEqualTo("Untitled Comic");
        assertThat(result.scene()).isEqualTo(Scene.PLAIN);
        assertThat(result.caption()).isEmpty();
        assertThat(result.dialogue()).isEmpty();
        assertThat(result.characters().getFirst().pose()).isEqualTo(Pose.STANDING);
    }

    @Test
    void rejectsAStoryWithNoPanels() {
        assertThatThrownBy(() -> normalizer.normalize(new ComicStoryResponse("T", List.of())))
                .isInstanceOf(ComicGenerationException.class);
    }
}
