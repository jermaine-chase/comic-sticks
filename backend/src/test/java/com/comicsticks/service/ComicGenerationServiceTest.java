package com.comicsticks.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.comicsticks.config.ComicProperties;
import com.comicsticks.dto.ComicOptions;
import com.comicsticks.dto.ComicStoryResponse;
import com.comicsticks.support.TestProperties;
import com.comicsticks.dto.Expression;
import com.comicsticks.dto.Pose;
import com.comicsticks.exception.ComicGenerationException;

import tools.jackson.databind.json.JsonMapper;

class ComicGenerationServiceTest {

    private static final String STORY_JSON = """
            {"title":"T","panels":[{"panelNumber":1,"scene":"outdoor","caption":"",
             "characters":[{"name":"Mia","pose":"waving","expression":"happy","facing":"right"}],
             "dialogue":[{"speaker":"Mia","text":"Hi!","type":"speech"}]}]}""";

    private MockRestServiceServer server;
    private ComicGenerationService service;

    @BeforeEach
    void setUp() {
        service = serviceWithKey("test-key");
    }

    private ComicStoryResponse generate(String text) {
        return service.generate(text, ComicOptions.DEFAULTS);
    }

    private ComicGenerationService serviceWithKey(String key) {
        ComicProperties properties = TestProperties.properties(
                TestProperties.llm("https://llm.test/v1", key), TestProperties.story(12));
        RestClient.Builder builder = RestClient.builder().baseUrl(properties.llm().baseUrl());
        server = MockRestServiceServer.bindTo(builder).build();
        return new ComicGenerationService(properties, new StorySegmenter(properties), builder.build());
    }

    private static String chatReply(String content, String finishReason) {
        return JsonMapper.builder().build().writeValueAsString(Map.of("choices", java.util.List.of(Map.of(
                "finish_reason", finishReason,
                "message", Map.of("role", "assistant", "content", content)))));
    }

    @Test
    void sendsStrictSchemaRequestAndParsesReply() {
        server.expect(requestTo("https://llm.test/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("test-model"))
                .andExpect(jsonPath("$.response_format.json_schema.strict").value(true))
                .andExpect(jsonPath("$.response_format.json_schema.schema.properties.panels.items"
                        + ".properties.characters.items.properties.pose.enum[0]").value("standing"))
                .andExpect(jsonPath("$.messages[1].content").value(startsWith("<story>\nOnce upon a time\n</story>")))
                .andRespond(withSuccess(chatReply(STORY_JSON, "stop"), MediaType.APPLICATION_JSON));

        ComicStoryResponse story = generate("Once upon a time");

        assertThat(story.title()).isEqualTo("T");
        assertThat(story.panels().getFirst().characters().getFirst().pose()).isEqualTo(Pose.WAVING);
        server.verify();
    }

    @Test
    void generatesEachParagraphSeparatelyAndJoinsTheResults() {
        String partOne = "Mia found the old map in the attic.";
        String partTwo = "Leo followed the map to the cave.";
        String secondJson = STORY_JSON.replace("\"T\"", "\"Other title\"").replace("Hi!", "Bye!");
        server.expect(requestTo("https://llm.test/v1/chat/completions"))
                .andExpect(jsonPath("$.messages[1].content").value(containsString(partOne)))
                .andExpect(jsonPath("$.messages[1].content").value(not(containsString(partTwo))))
                .andExpect(jsonPath("$.messages[1].content").value(containsString("part 1 of 2")))
                .andExpect(jsonPath("$.messages[1].content").value(containsString("do not wrap it up")))
                .andRespond(withSuccess(chatReply(STORY_JSON, "stop"), MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://llm.test/v1/chat/completions"))
                .andExpect(jsonPath("$.messages[1].content").value(containsString(partTwo)))
                .andExpect(jsonPath("$.messages[1].content").value(containsString("part 2 of 2")))
                .andExpect(jsonPath("$.messages[1].content").value(containsString("Characters so far: Mia")))
                .andExpect(jsonPath("$.messages[1].content").value(containsString("how the story ends")))
                .andRespond(withSuccess(chatReply(secondJson, "stop"), MediaType.APPLICATION_JSON));

        ComicStoryResponse story = generate(partOne + "\n\n" + partTwo);

        assertThat(story.title()).isEqualTo("T");
        assertThat(story.panels()).hasSize(2);
        assertThat(story.panels().getLast().dialogue().getFirst().text()).isEqualTo("Bye!");
        server.verify();
    }

    @Test
    void putsEachPartsExactTextUnderItsLastPanelAndCoversTheWholeStory() {
        String panel = STORY_JSON.substring(STORY_JSON.indexOf("{\"panelNumber"), STORY_JSON.lastIndexOf(']'));
        String two = "{\"title\":\"T\",\"panels\":[" + panel + "," + panel + "]}";
        String noPanels = "{\"title\":\"T\",\"panels\":[]}";
        String partOne = "Mia found the old map\nin the attic, under a pile of coats.";
        String partTwo = "Leo followed the map to the cave.";
        server.expect(requestTo("https://llm.test/v1/chat/completions"))
                .andRespond(withSuccess(chatReply(two, "stop"), MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://llm.test/v1/chat/completions"))
                .andRespond(withSuccess(chatReply(noPanels, "stop"), MediaType.APPLICATION_JSON));

        ComicStoryResponse story = generate(partOne + "\n\n" + partTwo);

        assertThat(story.panels()).extracting(p -> p.sourceText()).endsWith(partOne, partTwo);
        assertThat(story.panels().stream().map(p -> p.sourceText()).filter(s -> !s.isEmpty()))
                .containsExactly(partOne, partTwo);
        server.verify();
    }

    @Test
    void ignoresAnySourceTextTheLlmSends() {
        String json = STORY_JSON.replace("\"dialogue\"", "\"sourceText\":\"Made up words\",\"dialogue\"");
        server.expect(requestTo("https://llm.test/v1/chat/completions"))
                .andRespond(withSuccess(chatReply(json, "stop"), MediaType.APPLICATION_JSON));

        assertThat(generate("Once upon a time").panels().getFirst().sourceText())
                .isEqualTo("Once upon a time");
    }

    @Test
    void aFailedPartFailsTheComic() {
        server.expect(requestTo("https://llm.test/v1/chat/completions"))
                .andRespond(withSuccess(chatReply(STORY_JSON, "stop"), MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://llm.test/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> generate("Mia found the old map in the attic.\n\nLeo followed it to the cave."))
                .isInstanceOf(ComicGenerationException.class);
    }

    @Test
    void trimsAPartThatOverrunsItsBudgetButKeepsItsLastPanel() {
        String panel = STORY_JSON.substring(STORY_JSON.indexOf("{\"panelNumber"), STORY_JSON.lastIndexOf(']'));
        String three = "{\"title\":\"T\",\"panels\":[" + panel + "," + panel + ","
                + panel.replace("Hi!", "The end") + "]}";
        server.expect(requestTo("https://llm.test/v1/chat/completions"))
                .andRespond(withSuccess(chatReply(three, "stop"), MediaType.APPLICATION_JSON));

        ComicStoryResponse story = generate("Tiny story of one paragraph.");

        assertThat(story.panels()).hasSize(1);
        assertThat(story.panels().getFirst().dialogue().getFirst().text()).isEqualTo("The end");
    }

    @Test
    void estimateCountsPartsAndPanelsWithoutCallingTheLlm() {
        var estimate = service.estimate("Mia found the old map in the attic.\n\nLeo followed it to the cave.", ComicOptions.DEFAULTS);

        assertThat(estimate.parts()).isEqualTo(2);
        assertThat(estimate.panels()).isEqualTo(2);
        assertThat(estimate.estimatedSeconds()).isEqualTo(2 * 2 + 2 * 3);
        server.verify();
    }

    @Test
    void unknownEnumValuesFallBackToDefaults() {
        String json = STORY_JSON.replace("\"waving\"", "\"moonwalking\"").replace("\"happy\"", "\"smug\"");
        server.expect(requestTo("https://llm.test/v1/chat/completions"))
                .andRespond(withSuccess(chatReply(json, "stop"), MediaType.APPLICATION_JSON));

        var character = generate("x").panels().getFirst().characters().getFirst();

        assertThat(character.pose()).isEqualTo(Pose.STANDING);
        assertThat(character.expression()).isEqualTo(Expression.NEUTRAL);
    }

    @Test
    void missingApiKeyIsAClearError() {
        ComicGenerationService noKey = serviceWithKey("");

        assertThatThrownBy(() -> noKey.generate("x", ComicOptions.DEFAULTS))
                .isInstanceOfSatisfying(ComicGenerationException.class,
                        e -> assertThat(e.status()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void truncatedReplyIsReported() {
        server.expect(requestTo("https://llm.test/v1/chat/completions"))
                .andRespond(withSuccess(chatReply("{\"title\":", "length"), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> generate("x"))
                .isInstanceOf(ComicGenerationException.class)
                .hasMessageContaining("too long");
    }

    @Test
    void malformedJsonIsReported() {
        server.expect(requestTo("https://llm.test/v1/chat/completions"))
                .andRespond(withSuccess(chatReply("not json", "stop"), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> generate("x"))
                .isInstanceOfSatisfying(ComicGenerationException.class,
                        e -> assertThat(e.status()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }

    @Test
    void providerErrorBodyIsNotExposedToTheUser() {
        server.expect(requestTo("https://llm.test/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("secret provider detail"));

        assertThatThrownBy(() -> generate("x"))
                .isInstanceOf(ComicGenerationException.class)
                .hasMessageContaining("API key")
                .hasMessageNotContaining("secret provider detail");
    }
}
