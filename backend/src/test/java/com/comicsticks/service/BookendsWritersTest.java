package com.comicsticks.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.comicsticks.config.ComicProperties;
import com.comicsticks.dto.ChapterBookends;
import com.comicsticks.dto.ChapterBookends.AnswerChoice;
import com.comicsticks.dto.ChapterBookends.RecapItem;
import com.comicsticks.dto.CharacterDto;
import com.comicsticks.dto.ComicOptions;
import com.comicsticks.dto.ComicStoryResponse;
import com.comicsticks.dto.DialogueDto;
import com.comicsticks.dto.DialogueType;
import com.comicsticks.dto.Expression;
import com.comicsticks.dto.Facing;
import com.comicsticks.dto.PanelDto;
import com.comicsticks.dto.Pose;
import com.comicsticks.dto.Scene;
import com.comicsticks.dto.SupportLevel;
import com.comicsticks.support.TestProperties;

import tools.jackson.databind.json.JsonMapper;

class BookendsWritersTest {

    private static final String LLM_URL = "https://llm.test/v1/chat/completions";
    private static final ComicOptions WITH_QUESTIONS = new ComicOptions(SupportLevel.PARAGRAPH, true, true, true);

    private static CharacterDto figure(String name) {
        return new CharacterDto(name, Pose.STANDING, Expression.NEUTRAL, Facing.RIGHT, "");
    }

    private static final ComicStoryResponse STORY = new ComicStoryResponse("T", List.of(
            new PanelDto(1, Scene.OUTDOOR, "", List.of(figure("Mole")), List.of(), "Mole got lost in the wood.",
                    List.of(), "The wood"),
            new PanelDto(2, Scene.NIGHT, "", List.of(figure("Rat"), figure("Mole")),
                    List.of(new DialogueDto("Rat", "Mole! Over here!", DialogueType.SHOUT)),
                    "\"Mole! Over here!\" shouted Rat.", List.of(), ""),
            new PanelDto(3, Scene.NIGHT, "", List.of(figure("Rat"), figure("Mole")), List.of(),
                    "They walked home together.", List.of(), ""),
            new PanelDto(4, Scene.INDOOR, "", List.of(figure("Mole")), List.of(), "Mole slept by the fire.",
                    List.of(), "")));

    private static String chatReply(String content) {
        return JsonMapper.builder().build().writeValueAsString(Map.of("choices", List.of(Map.of(
                "finish_reason", "stop", "message", Map.of("role", "assistant", "content", content)))));
    }

    @Test
    void llmWriterChecksEveryChoiceAndWritesTheEndingItself() {
        ComicProperties properties = TestProperties.properties(12);
        RestClient.Builder builder = RestClient.builder().baseUrl(properties.llm().baseUrl());
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var writer = new LlmBookendsWriter(new LlmClient(properties, builder.build()), properties);
        server.expect(requestTo(LLM_URL))
                .andExpect(jsonPath("$.response_format.json_schema.name").value("chapter_recap"))
                .andExpect(jsonPath("$.messages[1].content").value(containsString("Panel 2: \"Mole! Over here!\"")))
                .andExpect(jsonPath("$.response_format.json_schema.schema.properties.mainCharacter.enum[0]")
                        .value("Mole"))
                .andRespond(withSuccess(chatReply("""
                        {"whereWeAre":"We are in the wood at night.",
                         "recap":[{"panelNumber":2,"sentence":"Rat found Mole"},
                                  {"panelNumber":42,"sentence":"Something that never happened."},
                                  {"panelNumber":3,"sentence":"They walked home."}],
                         "mainCharacter":"Mole","endingFeeling":"safe and warm"}"""), MediaType.APPLICATION_JSON));
        server.expect(requestTo(LLM_URL))
                .andExpect(jsonPath("$.response_format.json_schema.name").value("chapter_questions"))
                .andRespond(withSuccess(chatReply("""
                        {"questions":[{"type":"who","question":"Who found Mole","answerCharacter":"Rat","answerPanel":0},
                                      {"type":"picture","question":"Which picture shows Mole asleep?","answerCharacter":"","answerPanel":4},
                                      {"type":"who","question":"Who is Toad?","answerCharacter":"","answerPanel":0}]}"""),
                        MediaType.APPLICATION_JSON));

        ChapterBookends bookends = writer.write(STORY, WITH_QUESTIONS);

        assertThat(bookends.whosWho().characters()).containsExactly("Mole", "Rat");
        assertThat(bookends.whosWho().whereWeAre()).isEqualTo("We are in the wood at night.");
        assertThat(bookends.recap()).extracting(RecapItem::sentence)
                .containsExactly("Rat found Mole.", "They walked home.", "At the end, Mole feels safe.");
        assertThat(bookends.questions()).hasSize(2);
        assertThat(bookends.questions().getFirst().question()).isEqualTo("Who found Mole?");
        var picture = bookends.questions().get(1);
        assertThat(picture.choices().get(picture.answerIndex())).isEqualTo(new AnswerChoice(null, 4));
        server.verify();
    }

    @Test
    void demoWriterFillsEveryFieldByRule() {
        ChapterBookends bookends = new DemoBookendsWriter().write(STORY, WITH_QUESTIONS);

        assertThat(bookends.whosWho().whereWeAre()).isEqualTo("We are at the wood.");
        assertThat(bookends.recap()).hasSizeBetween(BookendsRules.MIN_RECAP, BookendsRules.MAX_RECAP);
        assertThat(bookends.recap().getLast().sentence()).isEqualTo("At the end, Mole feels calm.");
        assertThat(bookends.questions()).extracting(q -> q.question())
                .containsExactly("Who said \"Mole! Over here!\"?", "Which picture shows the end of the chapter?");
        var who = bookends.questions().getFirst();
        assertThat(who.choices().get(who.answerIndex()).character()).isEqualTo("Rat");
    }

    @Test
    void questionsAreLeftOutWhenTheyAreOff() {
        assertThat(new DemoBookendsWriter().write(STORY, ComicOptions.DEFAULTS).questions()).isEmpty();
    }
}
