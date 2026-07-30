package dev.serpinsights;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Free, keyless search over real published papers via CrossRef's works API —
 * the SERP source for the crime-reporting-features pipeline. Most entries
 * are behind publisher paywalls, so instead of fetching the full page,
 * feature extraction runs directly against the title + abstract CrossRef
 * already returns (when present).
 */
@Component
class CrossRefClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    List<SearchResult> search(String query, int maxResults) throws IOException, InterruptedException {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://api.crossref.org/works?query=%s&rows=%d&select=title,DOI,abstract,URL"
                .formatted(encodedQuery, maxResults);

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("CrossRef API returned HTTP " + response.statusCode());
        }

        JsonNode items = objectMapper.readTree(response.body()).path("message").path("items");
        List<SearchResult> results = new ArrayList<>();
        for (JsonNode item : items) {
            String title = item.path("title").isArray() && item.path("title").size() > 0
                    ? item.path("title").get(0).asText("")
                    : "";
            String url_ = item.path("URL").asText("");
            String abstractText = stripJatsTags(item.path("abstract").asText(""));
            results.add(new SearchResult(title, url_, abstractText));
        }
        return results;
    }

    private String stripJatsTags(String jats) {
        return jats.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }
}
