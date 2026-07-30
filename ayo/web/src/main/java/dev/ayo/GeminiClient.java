package dev.ayo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/** Thin wrapper around Gemini's generateContent REST endpoint, with a JSON response schema. */
@Component
class GeminiClient {

    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

    private final String apiKey;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(8))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    GeminiClient(@Value("${gemini.api-key}") String apiKey) {
        this.apiKey = apiKey;
    }

    /** Sends {@code prompt}, constrained to {@code schema} (an OpenAPI-style JSON schema), and returns the parsed JSON reply. */
    JsonNode generateJson(String prompt, Map<String, Object> schema) throws IOException, InterruptedException {
        Map<String, Object> body = Map.of(
                "contents", new Object[]{ Map.of("parts", new Object[]{ Map.of("text", prompt) }) },
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseSchema", schema,
                        // gemini-2.5-flash's default extended-thinking mode takes 30-40s+ for
                        // even a simple prompt; a game move needs to come back in a couple of
                        // seconds, and a flat answer is plenty good for this use case.
                        "thinkingConfig", Map.of("thinkingBudget", 0)
                )
        );

        HttpRequest request = HttpRequest.newBuilder(URI.create(ENDPOINT + "?key=" + apiKey))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Gemini API returned HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText();
        return objectMapper.readTree(text);
    }
}
