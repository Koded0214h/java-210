package dev.serpinsights;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Counts, per tag, how many documents in a corpus carry it — the "categorise
 * by number of systems having the feature" step from the assignment — with one
 * worker thread processing each document concurrently. A ConcurrentHashMap of
 * AtomicIntegers is the shared structure every worker writes into, so no
 * external locking is needed: each worker only ever contends on the single
 * counter for the tag it's currently incrementing.
 */
@Component
class ConcurrentTagAggregator {

    AggregationResult aggregate(List<SourceDocument> documents) {
        int poolSize = Math.max(1, Math.min(documents.size(), Runtime.getRuntime().availableProcessors()));
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        ConcurrentHashMap<String, AtomicInteger> counts = new ConcurrentHashMap<>();

        try {
            List<Future<?>> futures = new ArrayList<>(documents.size());
            for (SourceDocument document : documents) {
                futures.add(executor.submit(() -> tallyDocument(document, counts)));
            }
            awaitAll(futures);
        } finally {
            executor.shutdown();
        }

        List<TagCount> ranked = counts.entrySet().stream()
                .map(entry -> new TagCount(entry.getKey(), entry.getValue().get()))
                .sorted(Comparator.comparingInt(TagCount::documentCount).reversed()
                        .thenComparing(TagCount::tag))
                .toList();

        return new AggregationResult(documents.size(), ranked);
    }

    private void tallyDocument(SourceDocument document, ConcurrentHashMap<String, AtomicInteger> counts) {
        for (String tag : document.tags()) {
            counts.computeIfAbsent(tag, t -> new AtomicInteger()).incrementAndGet();
        }
    }

    private void awaitAll(List<Future<?>> futures) {
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
                throw new RuntimeException("Aggregation task failed", e.getCause());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Aggregation interrupted", e);
            }
        }
    }
}
