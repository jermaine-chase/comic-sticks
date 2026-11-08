package com.comicsticks.service;

import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.comicsticks.config.ComicProperties;
import com.comicsticks.exception.ComicGenerationException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Makes one strict-schema call to an OpenAI-compatible {@code /chat/completions} endpoint and reads
 * the reply into a record. Every LLM call in the app goes through here, so they all share the same
 * error handling: provider errors become messages safe to show the user, and the provider's own
 * response body is only ever logged.
 */
@Component
@Profile("!demo")
public class LlmClient {

    private final ComicProperties.Llm llm;
    private final RestClient client;
    private final JsonMapper mapper;

    public LlmClient(ComicProperties properties, RestClient llmRestClient) {
        this.llm = properties.llm();
        this.client = llmRestClient;
        // Providers without strict schema support can return a value outside an enum. A figure
        // drawn in a default pose beats failing the whole comic.
        this.mapper = JsonMapper.builder()
                .enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    public void requireKey() {
        if (llm.apiKey() == null || llm.apiKey().isBlank()) {
            throw new ComicGenerationException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No LLM API key is configured. Set LLM_API_KEY on the server, or start it with the demo profile.");
        }
    }

    public <T> T complete(String system, String prompt, String schemaName, Map<String, Object> schema, Class<T> type) {
        return parse(firstChoice(call(system, prompt, schemaName, schema)), type);
    }

    private ChatResponse call(String system, String prompt, String schemaName, Map<String, Object> schema) {
        try {
            return client.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + llm.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "model", llm.model(),
                            "max_completion_tokens", llm.maxOutputTokens(),
                            "messages", List.of(
                                    Map.of("role", "system", "content", system),
                                    Map.of("role", "user", "content", prompt)),
                            "response_format", Map.of(
                                    "type", "json_schema",
                                    "json_schema", Map.of("name", schemaName, "strict", true, "schema", schema))))
                    .retrieve()
                    .body(ChatResponse.class);
        } catch (RestClientResponseException e) {
            throw mapProviderError(e);
        } catch (ResourceAccessException e) {
            throw new ComicGenerationException(HttpStatus.GATEWAY_TIMEOUT,
                    "The LLM provider did not respond in time. Please try again.", e);
        }
    }

    private static ComicGenerationException mapProviderError(RestClientResponseException e) {
        int status = e.getStatusCode().value();
        String message = switch (status) {
            case 401, 403 -> "The LLM provider rejected the API key. Check LLM_API_KEY on the server.";
            case 429 -> "The LLM provider's rate limit or quota was reached. Please try again later.";
            default -> "The LLM provider returned an error (HTTP " + status + ").";
        };
        // The provider's body is kept in the log only.
        Exception cause = new IllegalStateException(e.getResponseBodyAsString(), e);
        return new ComicGenerationException(HttpStatus.BAD_GATEWAY, message, cause);
    }

    private static Choice firstChoice(ChatResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new ComicGenerationException(HttpStatus.BAD_GATEWAY, "The LLM returned an empty response.");
        }
        return response.choices().getFirst();
    }

    private <T> T parse(Choice choice, Class<T> type) {
        if ("length".equals(choice.finishReason())) {
            throw new ComicGenerationException(HttpStatus.BAD_GATEWAY,
                    "The comic was too long for the LLM to finish. Try a shorter story.");
        }
        Message message = choice.message();
        if (message == null || message.refusal() != null) {
            throw new ComicGenerationException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "The LLM declined to adapt this story.");
        }
        if (message.content() == null || message.content().isBlank()) {
            throw new ComicGenerationException(HttpStatus.BAD_GATEWAY, "The LLM returned an empty response.");
        }
        try {
            return mapper.readValue(message.content(), type);
        } catch (JacksonException e) {
            throw new ComicGenerationException(HttpStatus.BAD_GATEWAY,
                    "The LLM returned malformed comic data. Please try again.", e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatResponse(List<Choice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(Message message, @JsonProperty("finish_reason") String finishReason) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(String content, String refusal) {
    }
}
