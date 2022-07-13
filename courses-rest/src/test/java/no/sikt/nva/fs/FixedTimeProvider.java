package no.sikt.nva.fs;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class FixedTimeProvider implements TimeProvider {

    private final ZonedDateTime time;

    public FixedTimeProvider(final int year, final int month, final int day) {
        this.time = ZonedDateTime.of(year, month, day, 0, 0, 0, 0, ZoneId.of("Europe/Oslo"));
    }

    @Override
    public ZonedDateTime getCurrentTime() {
        return time;
    }
}
