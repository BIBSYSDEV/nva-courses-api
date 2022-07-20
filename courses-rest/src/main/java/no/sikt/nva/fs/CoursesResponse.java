package no.sikt.nva.fs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Collections;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonTypeName("CourseList")
public class CoursesResponse {

    private final List<Course> courses;

    public CoursesResponse() {
        this.courses = Collections.emptyList();
    }

    @JsonCreator
    public CoursesResponse(@JsonProperty("courses") final List<Course> courses) {
        this.courses = courses;
    }

    public List<Course> getCourses() {
        return courses;
    }
}
