package dev.serpinsights;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Free, keyless search over real arXiv papers — the SERP source for the
 * deep-learning-headings pipeline. Result URLs point at ar5iv.labs.arxiv.org,
 * which renders the paper's actual HTML (with real section headings)
 * instead of arxiv.org/abs's metadata-only page.
 */
@Component
class ArxivClient {

    private static final Pattern ARXIV_ID = Pattern.compile("abs/([^v]+)");
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    List<SearchResult> search(String query, int maxResults) throws IOException, InterruptedException {
        String andQuery = java.util.Arrays.stream(query.trim().split("\\s+"))
                .map(word -> "abs:" + word)
                .reduce((a, b) -> a + " AND " + b)
                .orElse("abs:" + query);
        String encodedQuery = URLEncoder.encode(andQuery, StandardCharsets.UTF_8);
        String url = "https://export.arxiv.org/api/query?search_query=%s&start=0&max_results=%d&sortBy=relevance"
                .formatted(encodedQuery, maxResults);

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("arXiv API returned HTTP " + response.statusCode());
        }

        Document feed = Jsoup.parse(response.body(), "", Parser.xmlParser());
        List<SearchResult> results = new ArrayList<>();
        for (Element entry : feed.select("entry")) {
            String absId = entry.selectFirst("id") != null ? entry.selectFirst("id").text() : "";
            String title = entry.selectFirst("title") != null ? entry.selectFirst("title").text().trim() : "";
            String summary = entry.selectFirst("summary") != null ? entry.selectFirst("summary").text().trim() : "";
            String ar5ivUrl = toAr5ivUrl(absId);
            if (!ar5ivUrl.isEmpty()) {
                results.add(new SearchResult(title, ar5ivUrl, summary));
            }
        }
        return results;
    }

    private String toAr5ivUrl(String absUrl) {
        Matcher matcher = ARXIV_ID.matcher(absUrl);
        if (!matcher.find()) {
            return "";
        }
        return "https://ar5iv.labs.arxiv.org/html/" + matcher.group(1);
    }
}
