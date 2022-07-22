package no.sikt.nva.fs;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class TimeProvider {

    public static final ZoneId ZONE_ID = ZoneId.of("Europe/Oslo");
    private final Clock clock;

    public TimeProvider(Clock clock) {
        this.clock = clock;
    }

    public int getYear() {
        return ZonedDateTime.now(clock).getYear();
    }

    public int getMonthValue() {
        return ZonedDateTime.now(clock).getMonthValue();
    }
}
