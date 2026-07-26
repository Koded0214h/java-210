# Ayo

A traditional Yoruba sowing/capture board game (also known as Ayoayo, closely
related to Oware and Awale). This repo contains two implementations of the
same game:

- [`cli/`](./cli) — a console version (single-file Java program), 2-player pass-and-play
- [`web/`](./web) — a Spring Boot + HTML/JS port, playable in a browser

## What is Ayo?

Ayo is played on a board of 12 pits arranged in two rows of 6 — one row per
player — plus a **store** on each side where captured seeds are banked.
Each pit starts with 4 seeds (48 seeds total).

```
        B6  B5  B4  B3  B2  B1
Store B                          Store A
        A1  A2  A3  A4  A5  A6
```

**Turns.** On your turn, pick one of your own non-empty pits. Its seeds are
picked up and "sown" one at a time into each following pit, moving
counter-clockwise (A1 → A2 → ... → A6 → B1 → ... → B6 → A1 → ...), skipping
back over the pit you started from if you have enough seeds to lap the board.

**Capturing.** If the very last seed you sow lands in an *opponent's* pit and
brings its count to exactly 2 or 3, you capture every seed in that pit into
your own store. Capturing chains backwards: if the pit just before that one
(still on the opponent's side) also has 2 or 3 seeds, you capture that one
too, and so on until the chain breaks.

**Ending the game.** Turns alternate until a player has no seeds left in any
of their own pits when it becomes their turn. The game ends immediately —
whatever seeds remain on the board go into the store of the side they're
sitting on. Whoever has more seeds in their store wins; equal stores is a
tie.

Both versions here are local 2-player pass-and-play — no AI opponent, no
networking.

## `cli/` — console version

A single-file Java program. Players take turns at the same terminal, entering
a pit number (1–6, relative to their own row) each turn. The board is
reprinted after every move.

Run it with:

```bash
cd cli
java Main.java
```

## `web/` — browser version

A Spring Boot backend (REST API) with a static HTML/CSS/JS frontend,
packaged as a single runnable jar. Game state (the board, stores, whose
turn it is) is kept server-side in the HTTP session — no database, nothing
persisted beyond the session. Click a pit on your side of the board to play
it; the app highlights which pits are currently playable.

Run it with:

```bash
cd web
mvn spring-boot:run
```

Then open `http://localhost:8080`.

### API

| Method | Path             | Description                                                      |
|--------|------------------|--------------------------------------------------------------------|
| POST   | `/api/game/new`  | Starts a new game, resets the board, resets the session           |
| POST   | `/api/game/move` | Body `{"pit": 0-11}` — sows from that pit and resolves captures   |

Pits are indexed `0`–`5` for Player A's row (A1–A6) and `6`–`11` for Player
B's row (B1–B6).
