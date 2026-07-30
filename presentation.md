# Presenting this tomorrow — CSC210 Group 9

A run sheet for walking through all 4 projects. Budget roughly 4–5 minutes
per project if you have ~20 minutes total; trim the demo time first if you're
short, not the concurrency explanation — that's what's actually being graded.

## Before you start

- Open all 4 apps in separate tabs beforehand (or have them ready to launch)
  so you're not waiting on `mvn spring-boot:run` mid-presentation. Cold
  Maven starts take 5–10s.
- Have a terminal ready to show `cd <project> && mvn spring-boot:run` briefly
  for at least one project — showing the actual command, not just the
  browser, signals this isn't a static mockup.
- Know your one-sentence answer to "why is this concurrent and not just
  multi-request?" for each project before you're asked it live.

## Opening (30 seconds)

"We built four projects for this assignment, each demonstrating a different
concurrency pattern from the course — a multithreaded aggregator, the Java
monitor pattern from JCiP, and two games that each ship both a console and a
browser version. All four are real, running Spring Boot apps, not slides."

---

## 1. SERP Insights (`serp-insights/`)

**What it does:** given a query, fetches a real search-engine results page,
then a thread pool concurrently fetches and mines *each individual result*
for either distinctive features (crime-reporting papers) or section headings
(deep-learning papers), then ranks them by how many results share each tag.

**The concurrency, in one sentence:** `ConcurrentTagAggregator` spins up a
fixed thread pool (one worker per fetched result, capped at core count) that
tally into a single shared `ConcurrentHashMap<String, AtomicInteger>` — each
worker only ever contends on the counter for the tag *it's* incrementing,
never on the map as a whole, so there's no external locking needed.

**Talking point if asked "why not just loop sequentially?":** each result
requires a real network fetch (arXiv/CrossRef + page HTML) — sequential
fetches would be I/O-bound and slow; the thread pool overlaps that latency.

**Demo:** run the "deep learning survey" query live. Point out the search →
fetch → extract → rank pipeline in the URL bar reference (arXiv/CrossRef,
not a canned dataset) and the real, ranked headings/features that come back.

**If it's slow or the query returns few results:** that's normal — CrossRef
in particular has a low hit rate for abstracts (~10%), which is why it
requests far more results than it needs. Don't apologize for it live, just
mention it's querying real external data, not a mock.

---

## 2. Fleet Vehicle Tracker (`vehicle-tracker/`)

**What it does:** implements the Java monitor pattern from *JCiP* §4.2.2 —
a shared `MonitorVehicleTracker` holds every vehicle's position, guarded
entirely by `synchronized`. Background "GPS" threads (one per vehicle)
mutate positions concurrently while the browser polls a consistent
snapshot once a second. The web version plots vehicles live on a Mapbox map
over Lagos.

**The concurrency, in one sentence:** thread-safety here comes from
*encapsulation*, not from `MutablePoint` being careful — the map is
deep-copied on construction and every read, and mutation only ever happens
under the monitor's lock, so no caller can ever observe or hold a reference
to a half-updated internal point.

**Talking point if asked "what's the one tradeoff":** `getLocations()` copies
the whole fleet on every call — fine at this scale (4 vehicles), but it's a
deliberate simplicity-over-throughput tradeoff you'd revisit for a huge
fleet read very frequently.

**Demo:** run the `cli/` version first (`java Main.java`) and type
`move V2 12 4` live to show a manual dispatcher thread racing against the
background updater threads in one terminal — this is the clearest way to
*see* the monitor pattern working. Then switch to the map to show the same
mechanism driving a nicer view.

---

## 3. Ayo (`ayo/`)

**What it does:** the traditional Yoruba sowing/capture board game, 2-player
pass-and-play, in both console and browser form. The web version adds an
"auto mode" where Gemini plays Player B — you play solo, Gemini reasons over
the board and picks a legal move each turn.

**Where's the concurrency here?** Be upfront: Ayo itself isn't the
concurrency example — it's a state-machine/game-logic exercise, same as
Dead or Wounded. If asked, say so plainly rather than reaching for a forced
answer. The genuinely concurrent projects are #1 and #2.

**Demo:** flip on auto mode and play a couple of moves against Gemini live —
this is the most fun part of the whole demo, use it. Mention the model call
is schema-constrained (always returns a valid `{pit, reason}`) and
server-side validated against actual legal moves before being applied, so a
bad or slow model response can never corrupt the game state.

---

## 4. Dead or Wounded (`dead-or-wounded/`)

**What it does:** a Mastermind/Bulls-and-Cows-style 4-digit code-breaking
game against a 60-second clock, console and browser versions.

**Where's the concurrency here?** In the `cli/` version specifically: a
background thread counts down 60 seconds while the main thread blocks
reading guesses from stdin — two threads, one shared deadline. The web
version moves the countdown to the browser (client-side JS timer) since
there's no shared multi-threaded state to demonstrate server-side once it's
a stateless per-session HTTP game.

**Demo:** this one has the most polished UI (CRT terminal theme, boot
sequence, ASCII timer bar) — good project to close on since it's visually
memorable. Actually play a round; don't just describe it.

---

## Closing (15 seconds)

"Two of the four — SERP Insights and the Vehicle Tracker — are the direct
concurrency demonstrations; Ayo and Dead or Wounded round out the
assignment with the console/web pairing and let us show off some of the
extra work we did beyond the base requirements, including a live AI
opponent and a real map integration."

## Anticipated questions

- **"Why Java monitor pattern and not `java.util.concurrent` locks?"** —
  that's literally the JCiP example being implemented (§4.2.2); the point is
  showing `synchronized` + encapsulation is sufficient here, not that it's
  the only option.
- **"What happens if two GPS threads write the same vehicle at once?"** —
  can't happen structurally: each vehicle only has one updater thread
  (`GpsSimulator` starts exactly one thread per vehicle ID), so there's
  no actual write race on a single vehicle, only concurrent writes across
  *different* vehicles plus concurrent reads from the view/dispatcher.
- **"Is the SERP data real or mocked?"** — real: arXiv's public search API
  and CrossRef's works API, both free and keyless, queried live.
- **"What stops the Gemini opponent from cheating/breaking the game?"** —
  server-side validation: every AI-proposed move is checked against the
  actual legal-move list before being applied; an invalid or failed model
  call falls back to the first legal pit.
