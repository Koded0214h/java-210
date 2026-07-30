package dev.serpinsights;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Heuristically detects which known features (see {@link FeatureVocabulary})
 * a fetched page's text mentions: a tag matches if its exact phrase appears
 * verbatim, or if every significant word (length >= 4) in the tag's phrase
 * appears somewhere in the page.
 */
@Component
class FeatureExtractor {

    private static final int MAX_TEXT_LENGTH = 20_000;

    private final FeatureVocabulary vocabulary;

    FeatureExtractor(FeatureVocabulary vocabulary) {
        this.vocabulary = vocabulary;
    }

    List<String> extract(String pageText) {
        String normalized = normalize(pageText);
        List<String> matches = new ArrayList<>();

        for (String tag : vocabulary.tags()) {
            String phrase = tag.replace('-', ' ');
            String[] words = phrase.split(" ");

            List<String> significantWords = Arrays.stream(words).filter(w -> w.length() >= 4).toList();
            boolean phrasePresent = normalized.contains(phrase);
            boolean allSignificantWordsPresent = !significantWords.isEmpty()
                    && significantWords.stream().allMatch(normalized::contains);

            if (phrasePresent || allSignificantWordsPresent) {
                matches.add(tag);
            }
        }
        return matches;
    }

    private String normalize(String pageText) {
        String truncated = pageText.length() > MAX_TEXT_LENGTH
                ? pageText.substring(0, MAX_TEXT_LENGTH)
                : pageText;
        return truncated.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", " ");
    }
}
