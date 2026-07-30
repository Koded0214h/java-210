package dev.serpinsights;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Known crime-reporting-system features to scan fetched pages for. Seeded from
 * the hand-read corpus so the live pipeline still recognizes the >=10 features
 * the assignment asks for, instead of guessing feature names out of thin air.
 */
@Component
class FeatureVocabulary {

    private final List<String> tags;

    FeatureVocabulary(CorpusLoader corpusLoader) {
        this.tags = corpusLoader.load("data/crime-reporting-systems.json").stream()
                .flatMap(doc -> doc.tags().stream())
                .distinct()
                .sorted()
                .toList();
    }

    List<String> tags() {
        return tags;
    }
}
