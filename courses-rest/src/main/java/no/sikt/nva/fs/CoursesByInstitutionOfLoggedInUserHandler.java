package no.sikt.nva.fs;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Optional;
import no.sikt.nva.fs.client.HttpException;
import no.sikt.nva.fs.config.FsConfig;
import no.sikt.nva.fs.config.InstitutionConfig;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import nva.commons.secrets.SecretsReader;

public class CoursesByInstitutionOfLoggedInUserHandler extends ApiGatewayHandler<Void, CoursesResponse> {

    /* default */ static final String FS_CONFIG_SECRET_NAME_ENV_NAME = "FsConfigSecretName";

    private final TimeProvider timeProvider;
    private final SecretsReader secretsReader;

    @JacocoGenerated
    public CoursesByInstitutionOfLoggedInUserHandler() {
        this(new Environment(), new SecretsReader(), Clock.system(ZoneId.systemDefault()));
    }

    public CoursesByInstitutionOfLoggedInUserHandler(final Environment environment,
                                                     final SecretsReader secretsReader,
                                                     final Clock clock) {
        super(Void.class, environment);
        this.timeProvider = new TimeProvider(clock);
        this.secretsReader = secretsReader;
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

    private CoursesResponse fetchInstitutionCourses(int institutionCode) throws FailedFsResponseException {
        final FsConfig fsConfig =
            secretsReader.fetchPlainTextJsonSecret(environment.readEnv(FS_CONFIG_SECRET_NAME_ENV_NAME), FsConfig.class);
        try {
            var institution = fetchInstitutionConfig(fsConfig, institutionCode);
            return fetchCoursesByInstitutionConfig(fsConfig.getBaseUri(), institution);
        } catch (InstitutionNotFoundException e) {
            return new CoursesResponse();
        }
    }

    private InstitutionConfig fetchInstitutionConfig(FsConfig fsConfig, int institutionCode)
        throws InstitutionNotFoundException {
        return fsConfig.getInstitutions().stream()
                   .filter(inst -> inst.getCode() == institutionCode)
                   .findFirst()
                   .orElseThrow(InstitutionNotFoundException::new);
    }

    private FailedFsResponseException handleFsFailingServerResponse(final HttpException httpException) {
        return new FailedFsResponseException(httpException);
    }

    private CoursesResponse fetchCoursesByInstitutionConfig(final String fsBaseUri,
                                                            final InstitutionConfig institutionConfig)
        throws FailedFsResponseException {
        try {
            final CoursesProvider coursesProvider = new CoursesProvider(fsBaseUri, institutionConfig);
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
