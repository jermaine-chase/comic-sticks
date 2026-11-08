package com.comicsticks.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@Profile("!demo")
public class LlmClientConfig {

    @Bean
    RestClient llmRestClient(RestClient.Builder builder, ComicProperties properties) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(properties.llm().timeout());
        return builder
                .baseUrl(properties.llm().baseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
