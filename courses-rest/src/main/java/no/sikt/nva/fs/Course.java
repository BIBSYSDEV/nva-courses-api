package no.sikt.nva.fs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;
import nva.commons.core.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public final class Course {

    private final String code;
    private final String term;
    private final int year;

    @JsonCreator
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

    @JacocoGenerated
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Course course = (Course) o;
        return year == course.year && code.equals(course.code) && term.equals(course.term);
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hash(code, term, year);
    }

    @JacocoGenerated
    @Override
    public String toString() {
        return "Course{"
               + "code='" + code + '\''
               + ", term='" + term + '\''
               + ", year=" + year
               + '}';
    }
}
