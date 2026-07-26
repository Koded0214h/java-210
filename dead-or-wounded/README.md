# Dead or Wounded

A 4-digit code-breaking game against the clock. This repo contains two
implementations of the same game:

- [`cli/`](./cli) — the original console version (single-file Java program)
- [`web/`](./web) — a Spring Boot + HTML/JS port of the same game, playable in a browser

## What is Dead or Wounded?

It's a "Bulls and Cows" / Mastermind-style guessing game. The game picks a
secret 4-digit code (digits `0`–`9`, repeats allowed). You have **60 seconds**
to guess it. After each guess you're told how close you were:

- **Dead** — a digit that's correct *and* in the correct position
- **Wounded** — a digit that exists in the secret but is in the wrong position

For example, if the secret is `4902` and you guess `4029`:

- `4` is in position 1 in both → **1 Dead**
- `0`, `2`, `9` are all in the secret but in different positions → **3 Wounded**
- Result: `1 Dead, 3 Wounded`

Guess all 4 digits in the correct position (`4 Dead, 0 Wounded`) before the
timer hits zero to win. If time runs out first, the secret is revealed and
the game ends.

## `cli/` — console version

A single-file Java program (`Main.java`). The secret is hardcoded (`4902`),
input is read from stdin, and a background thread counts down 60 seconds
while you type guesses.

Run it with:

```bash
cd cli
java Main.java
```

## `web/` — browser version

A Spring Boot backend (REST API) with a static HTML/CSS/JS frontend, packaged
as a single runnable jar. Differences from the console version:

- The secret is randomly generated per game (not hardcoded)
- Game state (secret, attempt count) is kept server-side in the HTTP session
  — no database, nothing persisted beyond the session
- The 60-second countdown runs client-side in the browser

Run it with:

```bash
cd web
mvn spring-boot:run
```

Then open `http://localhost:8080`.

### API

| Method | Path               | Description                                              |
|--------|--------------------|----------------------------------------------------------|
| POST   | `/api/game/new`    | Starts a new game, generates a secret, resets the session|
| POST   | `/api/game/guess`  | Body `{"guess":"1234"}` — evaluates a guess              |
| GET    | `/api/game/reveal` | Returns the secret for the active session's game         |
