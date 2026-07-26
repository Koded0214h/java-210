package dev.serpinsights;

import java.util.List;

/**
 * One paper/system from a corpus. {@code tags} holds its distinctive features
 * (crime-reporting corpus) or its section headings, in order (deep-learning corpus).
 */
public record SourceDocument(String name, List<String> tags) {
}
