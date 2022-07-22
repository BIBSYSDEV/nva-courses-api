package no.sikt.nva.fs.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class FsIdentifier {

    @JsonProperty("emne")
    private final FsCourse course;
    private final FsSemester semester;

    @JsonCreator
    public FsIdentifier(@JsonProperty("emne") final FsCourse course,
                        @JsonProperty("semester") final FsSemester semester) {
        this.course = course;
        this.semester = semester;
    }

    public FsCourse getCourse() {
        return course;
    }

    public FsSemester getSemester() {
        return semester;
    }
}
