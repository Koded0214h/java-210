package dev.serpinsights;

/** What happened to one real SERP result once its worker thread processed it. */
public record ResultOutcome(String title, String url, boolean used, int tagCount) {
}
