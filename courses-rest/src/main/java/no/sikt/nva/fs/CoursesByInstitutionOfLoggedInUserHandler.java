package no.sikt.nva.fs;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CoursesByInstitutionOfLoggedInUserHandler extends ApiGatewayHandler<Void, CoursesByInstitutionResponse> {

    private static final Logger logger = LoggerFactory.getLogger(CoursesByInstitutionOfLoggedInUserHandler.class);

    @JacocoGenerated
    public CoursesByInstitutionOfLoggedInUserHandler() {
        this(new Environment());
    }

    public CoursesByInstitutionOfLoggedInUserHandler(final Environment environment) {
        super(Void.class, environment);
    }

    @Override
    protected CoursesByInstitutionResponse processInput(final Void input,
                                                        final RequestInfo requestInfo,
                                                        final Context context) {

        final String institutionCode = "dummy";
        logger.debug("Fetching courses by institution '{}'", institutionCode);

        final CoursesByInstitutionResponse response = new CoursesByInstitutionResponse();
        response.setInstitutionCode(institutionCode);

        return response;
    }

    @Override
    protected Integer getSuccessStatusCode(Void input, CoursesByInstitutionResponse output) {
        return HttpURLConnection.HTTP_OK;
    }
}
