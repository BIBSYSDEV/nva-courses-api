package no.sikt.nva.fs;

import java.time.ZonedDateTime;

public interface TimeProvider {
    ZonedDateTime getCurrentTime();
}
