package no.sikt.nva.fs;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class SystemTimeProvider implements TimeProvider {
    private static final ZoneId ZONE_ID = ZoneId.of("Europe/Oslo");

    public SystemTimeProvider() {
    }

    @Override
    public ZonedDateTime getCurrentTime() {
        return ZonedDateTime.now(ZONE_ID);
    }
}
