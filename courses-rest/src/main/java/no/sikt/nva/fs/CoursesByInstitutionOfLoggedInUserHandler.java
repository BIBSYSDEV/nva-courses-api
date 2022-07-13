package no.sikt.nva.fs;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Optional;
import no.sikt.nva.fs.config.FsConfig;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.UnsupportedAcceptHeaderException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;

public class CoursesByInstitutionOfLoggedInUserHandler extends ApiGatewayHandler<Void, Course[]> {

    /* default */ static final String FS_CONFIG_ENV_NAME = "FS_CONFIG";

    private final TimeProvider timeProvider;

    @JacocoGenerated
    public CoursesByInstitutionOfLoggedInUserHandler() {
        this(new Environment(), new SystemTimeProvider());
    }

    public CoursesByInstitutionOfLoggedInUserHandler(final Environment environment, final TimeProvider timeProvider) {
        super(Void.class, environment);
        this.timeProvider = timeProvider;
    }

    @Override
    protected Course[] processInput(final Void input,
                                    final RequestInfo requestInfo,
                                    final Context context) {

        final Optional<Integer> institutionCode = getInstitutionCodeOfCurrentlyLoggedInUser(requestInfo);

        if (institutionCode.isPresent()) {
            final String fsConfigString = environment.readEnv(FS_CONFIG_ENV_NAME);

            try {
                final ObjectMapper objectMapper = getObjectMapper(requestInfo);
                final FsConfig fsConfig = objectMapper.readValue(fsConfigString.getBytes(StandardCharsets.UTF_8),
                                                                 FsConfig.class);
                return fsConfig.getInstitutions().stream()
                           .filter(inst -> inst.getCode() == institutionCode.get())
                           .findFirst()
                           .map(institutionConfig -> {
                               final CoursesProvider coursesProvider = new CoursesProvider(objectMapper,
                                                                                           fsConfig.getBaseUri(),
                                                                                           institutionConfig);
                               final ZonedDateTime currentTime = timeProvider.getCurrentTime();
                               return coursesProvider.getCurrentlyTaughtCourses(currentTime.getYear(),
                                                                                currentTime.getMonthValue());
                           })
                           .orElse(new Course[0]);
            } catch (IOException | UnsupportedAcceptHeaderException e) {
                throw new RuntimeException(e);
            }
        } else {
            return new Course[0];
        }
    }

    private Optional<Integer> getInstitutionCodeOfCurrentlyLoggedInUser(final RequestInfo requestInfo) {
        final Optional<URI> topLevelOrgCristinId = requestInfo.getTopLevelOrgCristinId();

        if (topLevelOrgCristinId.isPresent()) {
            final String lastPathElement = UriWrapper.fromUri(topLevelOrgCristinId.get())
                                               .getLastPathElement();
            final String[] parts = lastPathElement.split("\\.");
            return Optional.of(Integer.parseInt(parts[0]));
        } else {
            return Optional.empty();
        }
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, Course[] output) {
        return HttpURLConnection.HTTP_OK;
    }
}
