# Fleet Vehicle Tracker

An implementation of the **Java monitor pattern** example from *Java
Concurrency in Practice* (§4.2.2, "Example: tracking fleet vehicles"). This
repo contains two runnable demos of the same thread-safe tracker:

- [`cli/`](./cli) — a console dispatcher, all in one process
- [`web/`](./web) — a Spring Boot dashboard, tracker shared across HTTP requests

## What this is

A fleet vehicle tracker holds the current `(x, y)` location of every vehicle
in a fleet (taxis, delivery trucks, etc.). Two kinds of threads use it at
once:

- **Updater threads** — GPS feeds (or a dispatcher typing/clicking a manual
  correction) call `setLocation(id, x, y)`.
- **A view thread** — a UI reads `getLocations()` and renders the whole
  fleet's positions.

Both need to happen concurrently without corrupting data or letting the view
see a half-updated fleet. The fix is the **Java monitor pattern**:

- `MutablePoint` — a plain, *not* thread-safe `{x, y}` holder.
- `MonitorVehicleTracker` — wraps a `Map<String, MutablePoint>` and guards
  every access with `synchronized`. It never lets a caller touch its internal
  map or `MutablePoint`s directly:
  - the constructor deep-copies the map it's given,
  - `getLocations()` returns a deep copy (a consistent snapshot, frozen at
    the moment it was taken),
  - `getLocation(id)` returns a copy of one point,
  - `setLocation(id, x, y)` mutates the internal point under the lock.

Because nothing internal is ever published, `MonitorVehicleTracker` is
thread-safe even though `MutablePoint` itself is not — safety comes entirely
from encapsulation plus the monitor's lock, not from `MutablePoint` being
careful.

The one tradeoff worth knowing: `getLocations()` copies the whole fleet on
every call. Fine at this scale; if the fleet were huge and read very
frequently, that copy is the cost this pattern accepts for its simplicity.

## `cli/` — console demo

Single-file Java program, single process:

- 4 vehicles (`V1`–`V4`) start at fixed positions.
- One background **updater thread per vehicle** nudges it by ±1 in x/y every
  ~1–2 seconds, simulating a GPS feed.
- A **dispatcher thread** reads stdin for manual corrections.
- The **main thread** is the view: it re-renders a snapshot of the whole
  fleet once a second.

Run it with:

```bash
cd cli
java Main.java
```

While it's running, type:

```
move V2 12 4
```

to dispatch vehicle `V2` to `(12, 4)`, or `quit` to stop.

## `web/` — browser dashboard

A single `MonitorVehicleTracker` lives as one Spring singleton bean, shared
by every request — this mirrors the book's scenario more literally than a
per-session model would, since here there's genuinely one fleet watched by
possibly many viewers/dispatchers at once.

- `GpsSimulator` starts one background thread per vehicle on app startup —
  the updater threads.
- The browser page polls `GET /api/vehicles` once a second and renders each
  vehicle as a Mapbox marker (the view thread) — the tracker's abstract
  `(x, y)` grid is linearly mapped onto a real bounding box over Lagos
  (`gridToLngLat` in `script.js`), so the fleet moves over real streets
  instead of a blank grid. Backend state and the API are unchanged — this
  is purely a presentation-layer mapping.
- The "Dispatch a correction" form calls `POST /api/vehicles/{id}/location`
  to set a vehicle's position manually, exactly like the simulated GPS
  threads do internally.

The map needs a Mapbox token, set via:
```bash
export MAPBOX_TOKEN=pk.your-token
```
`ConfigController` serves it to the frontend at runtime (`GET /api/config`)
so it never lives in committed source — even though `pk.` tokens are meant
for client-side use, keeping literal secrets out of git history is worth
doing regardless. Without it set, the dashboard falls back to the fleet
table only (no map).

Run it with:

```bash
cd web
mvn spring-boot:run
```

Then open `http://localhost:8080/vehicle-tracker/` (the app has
`server.servlet.context-path=/vehicle-tracker` set so it can sit behind a
single reverse proxy alongside the other three projects — see the
top-level `nginx.conf` and `DEPLOY.md`).

### API

| Method | Path                          | Description                                    |
|--------|-------------------------------|--------------------------------------------------|
| GET    | `/api/vehicles`               | Returns a snapshot `{id: {x, y}}` of the fleet   |
| POST   | `/api/vehicles/{id}/location` | Body `{"x": int, "y": int}` — sets that vehicle's location |
