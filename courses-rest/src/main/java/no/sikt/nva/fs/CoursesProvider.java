package no.sikt.nva.fs;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.Collator;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import no.sikt.nva.fs.client.FsClient;
import no.sikt.nva.fs.client.FsCourse;
import no.sikt.nva.fs.client.FsSemester;
import no.sikt.nva.fs.client.HttpUrlConnectionFsClient;
import no.sikt.nva.fs.client.Item;
import no.sikt.nva.fs.config.InstitutionConfig;
import nva.commons.core.attempt.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoursesProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoursesProvider.class);
    /* default */ static final String LOG_MESSAGE_PREFIX_FS_COMMUNICATION_PROBLEM = "Unable to communicate with FS "
                                                                                    + "API for ";

    private final ObjectMapper objectMapper;
    private final String fsBaseUri;
    private final InstitutionConfig institutionConfig;

    public CoursesProvider(final ObjectMapper objectMapper, final String fsBaseUri,
                           final InstitutionConfig institutionConfig) {

        this.objectMapper = objectMapper;
        this.fsBaseUri = fsBaseUri;
        this.institutionConfig = institutionConfig;
    }

    public Course[] getCurrentlyTaughtCourses(final int year, final int month) {

        LOGGER.debug("Fetching courses by institution '{}' from FS", institutionConfig.getCode());

        final FsClient fsClient = new HttpUrlConnectionFsClient(objectMapper, fsBaseUri, institutionConfig.getCode(),
                                                                institutionConfig.getUsername(),
                                                                institutionConfig.getPassword());

        final Map<Integer, List<Term>> yearsToTerms = findRelevantYearsAndTerms(year, month);

        final List<Course> courses = new ArrayList<>();

        final Comparator<Course> courseComparator = new CourseComparator();

        yearsToTerms.forEach((entryYear, terms) -> courses.addAll(
            fetchCoursesForYearFilteringOnTerm(fsClient, courseComparator, entryYear, terms)));

        return courses.toArray(new Course[0]);
    }

    private Map<Integer, List<Term>> findRelevantYearsAndTerms(final int year, final int month) {
        final Map<Integer, List<Term>> yearsToTerms = new ConcurrentHashMap<>();
        /*
            During the first half of the year, we look up all courses for the current year. In the last half of the
            year, we look up courses for the rest of the current year and the first half of the next year:
         */
        if (month > Month.JUNE.getValue()) {
            yearsToTerms.put(year, Term.getAfterIncluding(Term.FALL));
            yearsToTerms.put(year + 1, Term.getBeforeExcluding(Term.FALL));
        } else {
            yearsToTerms.put(year, Term.getAfterIncluding(Term.SPRING));
        }
        return yearsToTerms;
    }

    private List<Course> fetchCoursesForYearFilteringOnTerm(final FsClient fsClient,
                                                            final Comparator<Course> courseComparator, final int year,
                                                            final List<Term> terms) {
        final List<String> termCodes = terms.stream().map(Term::getCode).collect(Collectors.toList());

        return Try.attempt(() -> fsClient.getTaughtCourses(year)
                                     .getItems()
                                     .stream()
                                     .map(this::asCourse)
                                     .filter(course -> termCodes.contains(course.getTerm()))
                                     .sorted(courseComparator)
                                     .collect(Collectors.toList()))
                   .orElse((failure) -> {
                       final String message = String.format(LOG_MESSAGE_PREFIX_FS_COMMUNICATION_PROBLEM + "%d",
                                                            institutionConfig.getCode());
                       LOGGER.warn(message, failure.getException());
                       return Collections.<Course>emptyList();
                   });
    }

    private Course asCourse(final Item item) {
        final FsCourse course = item.getId().getCourse();
        final FsSemester semester = item.getId().getSemester();

        return new Course(course.getCode(), semester.getTerm(), semester.getYear());
    }

    private static class CourseComparator implements Comparator<Course> {

        private final Comparator<Term> termComparator = Comparator.comparingInt(Term::getSeqNo);

        @Override
        public int compare(Course course1, Course course2) {
            int result = Integer.compare(course1.getYear(), course2.getYear());
            if (result == 0) {
                final Term term1 = Term.fromCode(course1.getTerm());
                final Term term2 = Term.fromCode(course2.getTerm());
                result = termComparator.compare(term1, term2);
            }
            if (result == 0) {
                final Collator norwegianCollator = Collator.getInstance(new Locale("nb", "NO"));
                result = norwegianCollator.compare(course1.getCode(), course2.getCode());
            }

            return result;
        }
    }
}
