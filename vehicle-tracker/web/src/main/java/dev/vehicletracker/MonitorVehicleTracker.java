package dev.vehicletracker;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Java monitor pattern (JCiP Listing 4.4): guards a Map<String, MutablePoint> with the
 * tracker's own intrinsic lock. Callers never see the internal map or its MutablePoints —
 * every read and write goes through a synchronized method that copies data in or out.
 *
 * A single instance of this is shared, as a Spring singleton bean, by the view side
 * (clients polling GET /api/vehicles) and multiple updater threads (the simulated GPS
 * feeds in GpsSimulator, plus dispatcher requests via POST /api/vehicles/{id}/location).
 */
public class MonitorVehicleTracker {
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
