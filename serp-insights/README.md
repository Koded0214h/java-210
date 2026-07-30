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

## Live SERP mode

The dashboard is search-driven: type a query into either panel and it runs
the actual assignment pipeline — fetch a real SERP, then concurrently mine
each result (one worker thread per result), then rank the extracted tags
with `ConcurrentTagAggregator`, same as before. **No API key, signup, or
billing account required** — both search sources are free and keyless.

There's no single general-web search API that's both free and keyless
(DuckDuckGo's HTML endpoint serves a bot-challenge to scripted requests;
Google/Bing require a paid or billing-linked account), so each panel uses a
domain-specific academic search API instead — which also happens to fit the
assignment's subject matter (real papers) better than generic web search:

- **Deep-learning headings** — `ArxivClient` searches arXiv
  (`export.arxiv.org/api/query`, free/unlimited/no key) for real papers,
  then `PageContentFetcher` fetches each one's actual rendered HTML from
  `ar5iv.labs.arxiv.org` (arXiv's HTML-rendering mirror), and
  `HeadingExtractor` pulls its real `<h2>`/`<h3>` section headings. Some
  older arXiv IDs have no ar5iv rendering and silently redirect back to the
  plain abs/metadata page — `HeadingExtractor` is scoped to ar5iv's
  `ltx_title` heading classes specifically, so that fallback page yields no
  tags instead of polluting results with sidebar nav text.
- **Crime-reporting features** — `CrossRefClient` searches CrossRef
  (`api.crossref.org/works`, free/unlimited/no key), which indexes papers
  across most publishers. Most matched papers are paywalled, so instead of
  fetching the full page, `FeatureExtractor` mines the title/abstract text
  CrossRef already returns (most results lack an abstract; the request asks
  for more results than needed to compensate).
- **Aggregate** — same `ConcurrentTagAggregator` as before, unchanged.

## Running it

```bash
cd serp-insights
mvn spring-boot:run
```

Then open `http://localhost:8080/serp-insights/` (the app has
`server.servlet.context-path=/serp-insights` set so it can sit behind a
single reverse proxy alongside the other three projects — see the
top-level `nginx.conf` and `DEPLOY.md`).

### API

| Method | Path                     | Description                                                              |
|--------|--------------------------|---------------------------------------------------------------------------|
| POST   | `/api/search/crime-features` | Body `{"query": "..."}` — live SERP + feature extraction, ranked        |
| POST   | `/api/search/dl-headings`    | Body `{"query": "..."}` — live SERP + heading extraction, ranked        |
| GET    | `/api/crime-features`   | Curated sample corpus (no network calls) — crime-reporting features       |
| GET    | `/api/dl-headings`      | Curated sample corpus (no network calls) — deep-learning sub-headings     |

`GET` endpoints return `{ documentCount, rankedTags: [{ tag, documentCount }, ...] }`.
`POST` endpoints return `{ query, resultsRequested, resultsUsed, resultsFailed, aggregation: {...same shape...} }`.
