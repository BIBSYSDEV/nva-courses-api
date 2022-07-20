package no.sikt.nva.fs;

import static nva.commons.core.attempt.Try.attempt;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Optional;
import no.sikt.nva.fs.client.HttpException;
import no.sikt.nva.fs.config.FsConfig;
import no.sikt.nva.fs.config.InstitutionConfig;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Try;
import nva.commons.core.paths.UriWrapper;

public class CoursesByInstitutionOfLoggedInUserHandler extends ApiGatewayHandler<Void, CoursesResponse> {

    public static final ObjectMapper OBJECT_MAPPER = JsonUtils.dtoObjectMapper;
    /* default */ static final String FS_CONFIG_ENV_NAME = "FS_CONFIG";

    private final FsConfig fsConfig;
    private final TimeProvider timeProvider;

    @JacocoGenerated
    public CoursesByInstitutionOfLoggedInUserHandler() {
        this(new Environment(), Clock.system(ZoneId.systemDefault()));
    }

    public CoursesByInstitutionOfLoggedInUserHandler(final Environment environment, final Clock clock) {
        super(Void.class, environment);
        this.timeProvider = new TimeProvider(clock);
        this.fsConfig = readFsConfig();
    }

    @Override
    protected CoursesResponse processInput(final Void input,
                                           final RequestInfo requestInfo,
                                           final Context context) throws FailedFsResponseException {

        var inst = getInstitutionCodeOfCurrentlyLoggedInUser(requestInfo);
        if (inst.isPresent()) {
            return fetchInstitutionCourses(inst.orElseThrow());
        }
        return new CoursesResponse();
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, CoursesResponse output) {
        return HttpURLConnection.HTTP_OK;
    }

    private FsConfig readFsConfig() {
        return environment.readEnvOpt(FS_CONFIG_ENV_NAME)
                   .map(attempt(configString -> OBJECT_MAPPER.readValue(configString, FsConfig.class)))
                   .map(Try::orElseThrow)
                   .orElseThrow();
    }

    private CoursesResponse fetchInstitutionCourses(int institutionCode)
        throws FailedFsResponseException {
        try {
            var institution = fetchInstitutionConfig(institutionCode);
            return fetchCoursesByInstitutionConfig(institution);
        } catch (InstitutionNotFoundException e) {
            return new CoursesResponse();
        }
    }

    private InstitutionConfig fetchInstitutionConfig(int institutionCode)
        throws InstitutionNotFoundException {
        return fsConfig.getInstitutions().stream()
                   .filter(inst -> inst.getCode() == institutionCode)
                   .findFirst()
                   .orElseThrow(InstitutionNotFoundException::new);
    }

    private FailedFsResponseException handleFsFailingServerResponse(final HttpException httpException) {
        return new FailedFsResponseException(httpException);
    }

    private CoursesResponse fetchCoursesByInstitutionConfig(final InstitutionConfig institutionConfig)
        throws FailedFsResponseException {
        try {
            final CoursesProvider coursesProvider = new CoursesProvider(fsConfig.getBaseUri(), institutionConfig);
            return new CoursesResponse(coursesProvider.getCurrentlyTaughtCourses(timeProvider.getYear(),
                                                                                 timeProvider.getMonthValue()));
        } catch (HttpException exception) {
            throw handleFsFailingServerResponse(exception);
        }
    }

    private Optional<Integer> getInstitutionCodeOfCurrentlyLoggedInUser(final RequestInfo requestInfo) {
        // We do not want to pick the FS institution code off the organizational URI like this, but
        // rather make it available directly in the RequestInfo object itself. Until this is supported,
        // we leave this as technical dept.
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
}
