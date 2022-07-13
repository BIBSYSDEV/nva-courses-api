package no.sikt.nva.fs;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import no.sikt.nva.fs.client.FsClient;
import no.sikt.nva.fs.client.HttpUrlConnectionFsClient;
import no.sikt.nva.fs.client.Item;
import no.sikt.nva.fs.config.InstitutionConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoursesProvider {

    private final static Logger LOGGER = LoggerFactory.getLogger(CoursesProvider.class);
    /* default */ static final String LOG_MESSAGE_PREFIX_FS_COMMUNICATION_PROBLEM =
        "Unable to communicate with FS API for ";

    private final ObjectMapper objectMapper;
    private final String fsBaseUri;
    private final InstitutionConfig institutionConfig;

    public CoursesProvider(final ObjectMapper objectMapper,
                           final String fsBaseUri,
                           final InstitutionConfig institutionConfig) {

        this.objectMapper = objectMapper;
        this.fsBaseUri = fsBaseUri;
        this.institutionConfig = institutionConfig;
    }

    public Course[] getCurrentlyTaughtCourses(final int year, final int month) {

        LOGGER.debug("Fetching courses by institution '{}' from FS", institutionConfig.getCode());

        try {
            final FsClient fsClient = new HttpUrlConnectionFsClient(objectMapper,
                                                                    fsBaseUri,
                                                                    institutionConfig.getCode(),
                                                                    institutionConfig.getUsername(),
                                                                    institutionConfig.getPassword());

            final Map<Integer, List<Term>> yearsToTerms = new ConcurrentHashMap<>();
            if (month > Month.JUNE.getValue()) {
                yearsToTerms.put(year, Term.getAfterIncluding(Term.FALL));
                yearsToTerms.put(year + 1, Term.getBeforeExcluding(Term.FALL));
            } else {
                yearsToTerms.put(year, Term.getAfterIncluding(Term.SPRING));
            }

            final List<Course> courses = new ArrayList<>();

            yearsToTerms.forEach((entryYear, terms) -> {
                final List<String> termCodes = terms.stream()
                                                   .map(Term::getCode)
                                                   .collect(Collectors.toList());
                final List<Course> coursesForYear = fsClient.getTaughtCourses(entryYear).getItems().stream()
                                                        .map(this::asCourse)
                                                        .filter(course -> termCodes.contains(course.getTerm()))
                                                        .collect(Collectors.toList());
                courses.addAll(coursesForYear);
            });

            return courses.toArray(new Course[0]);
        } catch (Exception e) {
            final String message = String.format(LOG_MESSAGE_PREFIX_FS_COMMUNICATION_PROBLEM + "%d",
                                                 institutionConfig.getCode());
            LOGGER.error(message, e);
            return new Course[0];
        }
    }

    private Course asCourse(final Item item) {
        final String lastPathElement = item.getHref().substring(item.getHref().lastIndexOf("/"));
        final String[] parts = lastPathElement.split(",");
        final int expectedNumberOfParts = 6;
        if (parts.length >= expectedNumberOfParts) {
            return new Course(URLDecoder.decode(parts[1], StandardCharsets.UTF_8),
                              URLDecoder.decode(parts[4], StandardCharsets.UTF_8),
                              Integer.parseInt(parts[3]));
        } else {
            throw new RuntimeException("Unable to split href in parts: " + item.getHref() + ", " + lastPathElement
                                       + ", " + Arrays.toString(parts));
        }
    }
}
