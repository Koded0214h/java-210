package dev.vehicletracker;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vehicles")
class VehicleController {

    private final MonitorVehicleTracker tracker;

    VehicleController(MonitorVehicleTracker tracker) {
        this.tracker = tracker;
    }

    record LocationDto(int x, int y) {
    }

    record SetLocationRequest(int x, int y) {
    }

    /** The "view thread" side: renders a consistent snapshot of the whole fleet. */
    @GetMapping
    Map<String, LocationDto> getVehicles() {
        return tracker.getLocations().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> new LocationDto(e.getValue().x, e.getValue().y)));
    }

    /** The dispatcher side: a manual correction, same as GpsSimulator's automated updates. */
    @PostMapping("/{id}/location")
    LocationDto setLocation(@PathVariable String id, @RequestBody SetLocationRequest request) {
        try {
            tracker.setLocation(id, request.x(), request.y());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
        MutablePoint updated = tracker.getLocation(id);
        return new LocationDto(updated.x, updated.y);
    }
}
