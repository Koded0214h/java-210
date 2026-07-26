import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Console demo of the Java monitor pattern from Java Concurrency in Practice, 4.2.2:
 * a thread-safe fleet vehicle tracker shared by a view thread (this program's
 * render loop) and multiple updater threads (simulated GPS feeds plus a
 * dispatcher typing manual corrections).
 */
public class Main {

    static final int MIN_X = 0, MAX_X = 30;
    static final int MIN_Y = 0, MAX_Y = 15;

    public static void main(String[] args) throws InterruptedException {
        Map<String, MutablePoint> initial = new HashMap<>();
        initial.put("V1", new MutablePoint(2, 2));
        initial.put("V2", new MutablePoint(10, 5));
        initial.put("V3", new MutablePoint(20, 10));
        initial.put("V4", new MutablePoint(5, 12));

        MonitorVehicleTracker tracker = new MonitorVehicleTracker(initial);
        AtomicBoolean running = new AtomicBoolean(true);

        System.out.println("=== FLEET VEHICLE TRACKER ===");
        System.out.println("Simulated GPS updater threads are moving each vehicle.");
        System.out.println("Type 'move <id> <x> <y>' to dispatch a manual correction, or 'quit' to stop.\n");

        for (String id : initial.keySet()) {
            Thread gpsThread = new Thread(() -> simulateGps(tracker, id, running), "gps-" + id);
            gpsThread.setDaemon(true);
            gpsThread.start();
        }

        Thread dispatcherThread = new Thread(() -> readDispatcherCommands(tracker, running), "dispatcher-input");
        dispatcherThread.setDaemon(true);
        dispatcherThread.start();

        while (running.get()) {
            renderFleet(tracker);
            Thread.sleep(1000);
        }

        System.out.println("\nStopped.");
    }

    /** Updater thread: periodically nudges one vehicle's position, simulating a GPS feed. */
    static void simulateGps(MonitorVehicleTracker tracker, String vehicleId, AtomicBoolean running) {
        while (running.get()) {
            try {
                Thread.sleep(800 + ThreadLocalRandom.current().nextInt(800));
            } catch (InterruptedException e) {
                return;
            }

            MutablePoint current = tracker.getLocation(vehicleId);
            if (current == null) {
                return;
            }

            int dx = ThreadLocalRandom.current().nextInt(-1, 2);
            int dy = ThreadLocalRandom.current().nextInt(-1, 2);
            int newX = clamp(current.x + dx, MIN_X, MAX_X);
            int newY = clamp(current.y + dy, MIN_Y, MAX_Y);
            tracker.setLocation(vehicleId, newX, newY);
        }
    }

    /** Dispatcher thread: reads manual location overrides from stdin. */
    static void readDispatcherCommands(MonitorVehicleTracker tracker, AtomicBoolean running) {
        try (Scanner scanner = new Scanner(System.in)) {
            while (running.get() && scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();

                if (line.equalsIgnoreCase("quit")) {
                    running.set(false);
                    break;
                }

                String[] parts = line.split("\\s+");
                if (parts.length != 4 || !parts[0].equalsIgnoreCase("move")) {
                    System.out.println("Usage: move <id> <x> <y>  (or 'quit')");
                    continue;
                }

                try {
                    String id = parts[1];
                    int x = Integer.parseInt(parts[2]);
                    int y = Integer.parseInt(parts[3]);
                    tracker.setLocation(id, clamp(x, MIN_X, MAX_X), clamp(y, MIN_Y, MAX_Y));
                    System.out.println("Dispatched " + id + " to (" + x + ", " + y + ")");
                } catch (NumberFormatException e) {
                    System.out.println("x and y must be numbers.");
                } catch (IllegalArgumentException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }

    static void renderFleet(MonitorVehicleTracker tracker) {
        Map<String, MutablePoint> snapshot = tracker.getLocations();
        System.out.println("---- Fleet snapshot ----");
        snapshot.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> System.out.printf(
                        "%-4s (%2d, %2d)%n", entry.getKey(), entry.getValue().x, entry.getValue().y));
        System.out.println();
    }

    static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

/** Mutable (x, y) location. NOT thread-safe on its own — safety comes from how MonitorVehicleTracker uses it. */
class MutablePoint {
    public int x, y;

    public MutablePoint() {
        x = 0;
        y = 0;
    }

    public MutablePoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public MutablePoint(MutablePoint p) {
        this.x = p.x;
        this.y = p.y;
    }
}

/**
 * Java monitor pattern (JCiP Listing 4.4): guards a Map<String, MutablePoint> with the
 * tracker's own intrinsic lock. Callers never see the internal map or its MutablePoints —
 * every read and write goes through a synchronized method that copies data in or out.
 */
class MonitorVehicleTracker {
    private final Map<String, MutablePoint> locations;

    public MonitorVehicleTracker(Map<String, MutablePoint> locations) {
        this.locations = deepCopy(locations);
    }

    public synchronized Map<String, MutablePoint> getLocations() {
        return deepCopy(locations);
    }

    public synchronized MutablePoint getLocation(String id) {
        MutablePoint loc = locations.get(id);
        return loc == null ? null : new MutablePoint(loc);
    }

    public synchronized void setLocation(String id, int x, int y) {
        MutablePoint loc = locations.get(id);
        if (loc == null) {
            throw new IllegalArgumentException("No such vehicle: " + id);
        }
        loc.x = x;
        loc.y = y;
    }

    private static Map<String, MutablePoint> deepCopy(Map<String, MutablePoint> m) {
        Map<String, MutablePoint> result = new HashMap<>();
        for (String id : m.keySet()) {
            result.put(id, new MutablePoint(m.get(id)));
        }
        return Collections.unmodifiableMap(result);
    }
}
