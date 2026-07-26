package dev.vehicletracker;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
class TrackerConfig {

    @Bean
    MonitorVehicleTracker vehicleTracker() {
        Map<String, MutablePoint> initial = new HashMap<>();
        initial.put("V1", new MutablePoint(2, 2));
        initial.put("V2", new MutablePoint(10, 5));
        initial.put("V3", new MutablePoint(20, 10));
        initial.put("V4", new MutablePoint(5, 12));
        return new MonitorVehicleTracker(initial);
    }
}
