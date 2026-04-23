package com.powersolutions.solarshield;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.TimeZone;

/**
 * Sets the JVM default timezone so application timestamps align with the New York service area.
 */
@Component
public class TimeZoneConfig {

    private static final String EASTERN_TIME_ZONE = "America/New_York";

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone(EASTERN_TIME_ZONE));
    }

}
