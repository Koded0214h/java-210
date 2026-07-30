package dev.serpinsights;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

/** Downloads one result page's HTML so it can be mined for headings/features. */
@Component
class PageContentFetcher {

    private static final String USER_AGENT =
            "Mozilla/5.0 (compatible; SerpInsightsBot/1.0; CSC210 class project)";

    Optional<Document> fetch(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(8000)
                    .followRedirects(true)
                    .get();
            return Optional.of(doc);
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
