package no.sikt.nva.fs.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FsSemester {
    @JsonProperty("ar")
    private final int year;
    @JsonProperty("termin")
    private final String term;

    @JsonCreator
    public FsSemester(@JsonProperty("ar") final int year, @JsonProperty("termin") final String term) {
        this.year = year;
        this.term = term;
    }

    public int getYear() {
        return year;
    }

    public String getTerm() {
        return term;
    }
}
