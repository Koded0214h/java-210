package dev.serpinsights;

import java.util.List;

/** Result of a live search-and-aggregate run, with per-result outcomes alongside the aggregation itself. */
public record LiveAggregationResult(
        String query,
        int resultsRequested,
        int resultsUsed,
        int resultsFailed,
        List<ResultOutcome> results,
        AggregationResult aggregation) {
}
