package dev.serpinsights;

/** A tag (feature or heading) ranked by how many documents in the corpus have it. */
public record TagCount(String tag, int documentCount) {
}
