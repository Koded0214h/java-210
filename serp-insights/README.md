# SERP Insights

A multithreaded Java/Spring Boot program that aggregates and visualizes two
things pulled from real papers found via search-engine results (SERP):

1. **Distinctive features of crime-reporting-system papers** — at least 10
   distinct features, categorized/ranked by how many systems have each one.
2. **Distinct sub-headings of deep-learning survey papers** — ranked by how
   many papers use each heading.

This is web-only (no CLI) — the visualization is the deliverable.

## Where the data comes from

Rather than fabricate "features" or "headings," every entry here was read out
of real downloaded papers (PDFs in `../docs/crime-reporting-papers/` and
`../docs/deep-learning-models/`):

- **Crime-reporting corpus** (`src/main/resources/data/crime-reporting-systems.json`) —
  14 distinct systems. Several papers are literature reviews that describe
  multiple prior systems in addition to their own proposed one, so 6 source
  documents yielded 14 separate systems, each with its own feature list
  (e.g. anonymous reporting, SOS panic button, evidence/photo upload, admin
  dashboard, complaint status tracking, AI-based case prioritization).
- **Deep-learning corpus** (`src/main/resources/data/deep-learning-papers.json`) —
  7 arXiv survey papers, each with its real top-level section headings in
  order (Introduction, Related Work/Background, ..., Conclusion). A couple of
  papers only had their earlier sections confirmed (noted where that's the
  case) rather than guessing at the rest.

Each JSON file is just `[{"name": ..., "tags": [...]}, ...]` — swap in more
papers by adding more entries in the same shape.

## The concurrency part

`ConcurrentTagAggregator` is where the "multithreaded" requirement actually
lives: given a corpus (a `List<SourceDocument>`), it spins up a fixed thread
pool sized to the corpus (capped at the machine's core count) and submits one
task per document. Each worker thread tallies that document's tags into a
single shared `ConcurrentHashMap<String, AtomicInteger>` — safe under
concurrent writes because every worker only ever contends on the counter for
the specific tag it's incrementing, never on the map as a whole. Once every
task completes, the counts are sorted descending into a ranked list — this
ranking *is* the "categorise by number of systems having the feature" step
from the assignment.

The same aggregator is reused for both corpora — it doesn't know or care
whether a "tag" is a feature or a heading.

## Running it

```bash
cd serp-insights
mvn spring-boot:run
```

Then open `http://localhost:8080`. The dashboard shows two ranked bar charts
(crime-reporting features, deep-learning headings), each with hover tooltips
and a table-view toggle.

### API

| Method | Path                | Description                                                      |
|--------|---------------------|--------------------------------------------------------------------|
| GET    | `/api/crime-features` | Crime-reporting features, ranked by number of systems having each |
| GET    | `/api/dl-headings`    | Deep-learning sub-headings, ranked by number of papers having each |

Both return `{ documentCount, rankedTags: [{ tag, documentCount }, ...] }`.
