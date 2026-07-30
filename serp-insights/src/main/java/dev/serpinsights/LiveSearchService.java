package dev.serpinsights;

import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * Runs the whole assignment pipeline for one query: fetch a real SERP, then
 * concurrently mine each result (one worker thread per result), then hand
 * the extracted per-result tags to {@link ConcurrentTagAggregator} for the
 * ranking step.
 *
 * <p>The two modes use different free, keyless search sources since no
 * single general-web search API is both free and keyless: HEADINGS searches
 * arXiv and fetches each paper's real HTML (via ar5iv) to pull real section
 * headings; FEATURES searches CrossRef and mines the title/abstract CrossRef
 * already returns, since most matched papers are behind publisher paywalls
 * and not worth trying to fetch directly.
 */
@Component
class LiveSearchService {

    private static final int ARXIV_MAX_RESULTS = 8;
    private static final int CROSSREF_MAX_RESULTS = 50;

    private final ArxivClient arxivClient;
    private final CrossRefClient crossRefClient;
    private final PageContentFetcher pageFetcher;
    private final HeadingExtractor headingExtractor;
    private final FeatureExtractor featureExtractor;
    private final ConcurrentTagAggregator aggregator;

    LiveSearchService(ArxivClient arxivClient,
                       CrossRefClient crossRefClient,
                       PageContentFetcher pageFetcher,
                       HeadingExtractor headingExtractor,
                       FeatureExtractor featureExtractor,
                       ConcurrentTagAggregator aggregator) {
        this.arxivClient = arxivClient;
        this.crossRefClient = crossRefClient;
        this.pageFetcher = pageFetcher;
        this.headingExtractor = headingExtractor;
        this.featureExtractor = featureExtractor;
        this.aggregator = aggregator;
    }

    LiveAggregationResult search(String query, ExtractionMode mode) throws IOException, InterruptedException {
        List<SearchResult> results = switch (mode) {
            case HEADINGS -> arxivClient.search(query, ARXIV_MAX_RESULTS);
            case FEATURES -> crossRefClient.search(query, CROSSREF_MAX_RESULTS);
        };
        int poolSize = Math.max(1, Math.min(results.size(), Runtime.getRuntime().availableProcessors()));
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);

        try {
            List<Future<Extraction>> futures = new ArrayList<>(results.size());
            for (SearchResult result : results) {
                futures.add(executor.submit(() -> fetchAndExtract(result, mode)));
            }

            List<SourceDocument> documents = new ArrayList<>();
            List<ResultOutcome> outcomes = new ArrayList<>(results.size());
            for (int i = 0; i < futures.size(); i++) {
                SearchResult original = results.get(i);
                Extraction extraction = awaitOne(futures.get(i), original);
                boolean used = !extraction.tags().isEmpty();
                outcomes.add(new ResultOutcome(original.title(), original.url(), used, extraction.tags().size()));
                if (used) {
                    documents.add(new SourceDocument(original.title(), extraction.tags()));
                }
            }

            AggregationResult aggregation = aggregator.aggregate(documents);
            return new LiveAggregationResult(query, results.size(), documents.size(),
                    results.size() - documents.size(), outcomes, aggregation);
        } finally {
            executor.shutdown();
        }
    }

    private record Extraction(List<String> tags) {
    }

    private Extraction fetchAndExtract(SearchResult result, ExtractionMode mode) {
        List<String> tags = switch (mode) {
            case HEADINGS -> {
                Optional<Document> page = pageFetcher.fetch(result.url());
                yield page.map(headingExtractor::extract).orElse(List.of());
            }
            case FEATURES -> featureExtractor.extract(result.snippet());
        };
        return new Extraction(tags);
    }

    private Extraction awaitOne(Future<Extraction> future, SearchResult original) throws InterruptedException {
        try {
            return future.get();
        } catch (ExecutionException e) {
            return new Extraction(List.of());
        }
    }
}
