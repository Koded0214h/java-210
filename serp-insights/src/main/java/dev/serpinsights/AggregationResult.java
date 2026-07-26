package dev.serpinsights;

import java.util.List;

public record AggregationResult(int documentCount, List<TagCount> rankedTags) {
}
