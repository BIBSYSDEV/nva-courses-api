package no.sikt.nva.fs;

import com.fasterxml.jackson.annotation.JsonProperty;

public final class Course {
    private final String code;
    private final String seasonNo;
    private final String year;

    public Course(@JsonProperty("code") final String code,
                  @JsonProperty("seasonNo") final String seasonNo,
                  @JsonProperty("year") String year) {

        this.code = code;
        this.seasonNo = seasonNo;
        this.year = year;
    }

    public final String getCode() {
        return code;
    }

    public final String getSeasonNo() {
        return seasonNo;
    }

    public final String getYear() {
        return year;
    }
}
