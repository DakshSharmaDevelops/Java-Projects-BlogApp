package com.blog.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
@SuppressWarnings("unchecked")
public class OpenAiService {

    private final WebClient webClient;
    private  final String apiKey;

    public OpenAiService(@Value("${openai.api.key}") String apiKey,
                         @Value("${openai.api.url}") String apiUrl) {
        this.apiKey = apiKey;
        this.webClient = WebClient.builder().baseUrl(apiUrl).build();
    }

    public String generateExcerptAndTags(String postContent) {
        String prompt = """
            Read this blog post content and respond ONLY in this exact format,
            nothing else:
            EXCERPT: <a 1-2 sentence summary, under 150 characters>
            TAGS: <3-5 comma separated lowercase tags>

            Content:
            """ + postContent;

        Map<String, Object> requestBody = Map.of(
                "model", "openai/gpt-oss-120b",
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", 0.5
        );

        Map response = webClient.post()
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block(); // blocking call — theek hai simple web app ke liye

        assert response != null;
        List<Map> choices = (List<Map>) response.get("choices");
        Map message = (Map) choices.get(0).get("message");
        return (String) message.get("content");
    }
}