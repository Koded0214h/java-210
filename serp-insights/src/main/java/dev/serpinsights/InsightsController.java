package dev.serpinsights;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
class InsightsController {

    private static final String CRIME_CORPUS = "data/crime-reporting-systems.json";
    private static final String DEEP_LEARNING_CORPUS = "data/deep-learning-papers.json";

    private final CorpusLoader corpusLoader;
    private final ConcurrentTagAggregator aggregator;
    private final LiveSearchService liveSearchService;

    InsightsController(CorpusLoader corpusLoader,
                        ConcurrentTagAggregator aggregator,
                        LiveSearchService liveSearchService) {
        this.corpusLoader = corpusLoader;
        this.aggregator = aggregator;
        this.liveSearchService = liveSearchService;
    }

    /** Distinctive features of crime-reporting systems, ranked by number of systems having each (curated sample corpus). */
    @GetMapping("/api/crime-features")
    AggregationResult crimeFeatures() {
        return aggregator.aggregate(corpusLoader.load(CRIME_CORPUS));
    }

    /** Distinct sub-headings of deep-learning papers, ranked by number of papers having each (curated sample corpus). */
    @GetMapping("/api/dl-headings")
    AggregationResult deepLearningHeadings() {
        return aggregator.aggregate(corpusLoader.load(DEEP_LEARNING_CORPUS));
    }

    /** Runs a live SERP + multithreaded feature-extraction pipeline for a crime-reporting-system query. */
    @PostMapping("/api/search/crime-features")
    ResponseEntity<?> searchCrimeFeatures(@RequestBody SearchRequest request) {
        return runSearch(request, ExtractionMode.FEATURES);
    }

    /** Runs a live SERP + multithreaded heading-extraction pipeline for a deep-learning-paper query. */
    @PostMapping("/api/search/dl-headings")
    ResponseEntity<?> searchDeepLearningHeadings(@RequestBody SearchRequest request) {
        return runSearch(request, ExtractionMode.HEADINGS);
    }

    private ResponseEntity<?> runSearch(SearchRequest request, ExtractionMode mode) {
        if (request.query() == null || request.query().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "query must not be blank"));
        }
        try {
            return ResponseEntity.ok(liveSearchService.search(request.query(), mode));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", "Search failed: " + e.getMessage()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Search interrupted"));
        }
    }
}
