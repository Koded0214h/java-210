package dev.vehicletracker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Hands the frontend its Mapbox token at runtime instead of baking it into committed JS. */
@RestController
class ConfigController {

    private final String mapboxToken;

    ConfigController(@Value("${mapbox.token}") String mapboxToken) {
        this.mapboxToken = mapboxToken;
    }

    record ConfigResponse(String mapboxToken) {
    }

    @GetMapping("/api/config")
    ConfigResponse config() {
        return new ConfigResponse(mapboxToken);
    }
}
