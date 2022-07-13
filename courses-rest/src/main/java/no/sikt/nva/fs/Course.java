package no.sikt.nva.fs;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class Course {
    private final String code;
    private final String term;
    private final int year;

    public Course(@JsonProperty("code") final String code,
                  @JsonProperty("term") final String term,
                  @JsonProperty("year") int year) {

        this.code = code;
        this.term = term;
        this.year = year;
    }

    public String getCode() {
        return code;
    }

    public String getTerm() {
        return term;
    }

    public int getYear() {
        return year;
    }
}
