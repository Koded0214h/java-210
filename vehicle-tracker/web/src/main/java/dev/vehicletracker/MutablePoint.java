package dev.vehicletracker;

/** Mutable (x, y) location. NOT thread-safe on its own — safety comes from how MonitorVehicleTracker uses it. */
public class MutablePoint {
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
