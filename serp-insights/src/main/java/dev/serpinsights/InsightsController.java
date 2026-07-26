package dev.serpinsights;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class InsightsController {

    private static final String CRIME_CORPUS = "data/crime-reporting-systems.json";
    private static final String DEEP_LEARNING_CORPUS = "data/deep-learning-papers.json";

    private final CorpusLoader corpusLoader;
    private final ConcurrentTagAggregator aggregator;

    InsightsController(CorpusLoader corpusLoader, ConcurrentTagAggregator aggregator) {
        this.corpusLoader = corpusLoader;
        this.aggregator = aggregator;
    }

    /** Distinctive features of crime-reporting systems, ranked by number of systems having each. */
    @GetMapping("/api/crime-features")
    AggregationResult crimeFeatures() {
        return aggregator.aggregate(corpusLoader.load(CRIME_CORPUS));
    }

    /** Distinct sub-headings of deep-learning papers, ranked by number of papers having each. */
    @GetMapping("/api/dl-headings")
    AggregationResult deepLearningHeadings() {
        return aggregator.aggregate(corpusLoader.load(DEEP_LEARNING_CORPUS));
    }
}
