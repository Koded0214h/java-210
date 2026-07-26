package dev.vehicletracker;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Spawns one background "updater thread" per vehicle that periodically nudges its
 * position, simulating a live GPS feed writing through MonitorVehicleTracker
 * concurrently with dispatcher requests and client reads.
 */
@Component
class GpsSimulator {

    static final int MIN_X = 0, MAX_X = 30;
    static final int MIN_Y = 0, MAX_Y = 15;

    private final MonitorVehicleTracker tracker;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private List<Thread> threads;

    GpsSimulator(MonitorVehicleTracker tracker) {
        this.tracker = tracker;
    }

    @PostConstruct
    void start() {
        threads = tracker.getLocations().keySet().stream()
                .map(id -> {
                    Thread thread = new Thread(() -> simulate(id), "gps-" + id);
                    thread.setDaemon(true);
                    thread.start();
                    return thread;
                })
                .toList();
    }

    @PreDestroy
    void stop() {
        running.set(false);
        threads.forEach(Thread::interrupt);
    }

    private void simulate(String vehicleId) {
        while (running.get()) {
            try {
                Thread.sleep(800 + ThreadLocalRandom.current().nextInt(1200));
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

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
