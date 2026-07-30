package dev.serpinsights;

/** One organic result from a search-engine results page. */
public record SearchResult(String title, String url, String snippet) {
}
