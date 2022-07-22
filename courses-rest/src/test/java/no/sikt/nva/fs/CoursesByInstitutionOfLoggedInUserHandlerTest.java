package no.sikt.nva.fs;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static no.sikt.nva.fs.TestConfig.restApiMapper;
import static no.sikt.nva.fs.client.HttpStatusException.UNEXPECTED_RESPONSE_CODE_RETURNED_BY_SERVER_MESSAGE;
import static no.sikt.nva.fs.client.HttpUrlConnectionFsClient.PROBLEMS_COMMUNICATING_WITH_SERVER_MESSAGE;
import static no.sikt.nva.fs.client.HttpUrlConnectionFsClient.PROBLEMS_READING_RESPONSE_FROM_SERVER_MESSAGE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyIterable.emptyIterable;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.collection.IsIterableWithSize.iterableWithSize;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

@WireMockTest
class CoursesByInstitutionOfLoggedInUserHandlerTest {

    public static final Clock AFTER_SUMMER = Clock.fixed(Instant.parse("2022-08-10T10:15:30.00Z"),
                                                         TimeProvider.ZONE_ID);
    public static final Clock BEFORE_SUMMER = Clock.fixed(Instant.parse("2022-03-10T00:00:00.00Z"),
                                                          TimeProvider.ZONE_ID);
    private static final String EXAMPLE_COM_URI = "http://example.com";
    private static final String NON_SUPPORTED_INSTITUTION_PATH = "100.0.0.0.0";
    private static final String SUPPORTED_INSTITUTION_PATH = "215.0.0.0.0";
    private static final String TAUGHT_COURSES_URL_PATH = "/undervisning";
    private static final String DB_ID_QUERY_PARAM_NAME = "dbId";
    private static final String INSTITUTION_QUERY_PARAM_NAME = "emne.institusjon";
    private static final String LIMIT_QUERY_PARAM_NAME = "limit";
    private static final String YEAR_QUERY_PARAM_NAME = "semester.ar";
    private static final String SUCCESS_AUTHORIZATION_HEADER_VALUE = "Basic ZHVtbXlVc2VybmFtZTpkdW1teVBhc3N3b3Jk";
    private static final String APPLICATION_JSON_CONTENT_TYPE_VALUE = "application/json";
    private static final Course COURSE_B_SPRING_2022 = new Course("B", "V\u00C5R", 2022); // VÅR
    private static final Course COURSE_OE_SPRING_2022 = new Course("\u00D8", "V\u00C5R", 2022); // Ø, VÅR
    private static final Course COURSE_AA_SPRING_2022 = new Course("\u00C5", "V\u00C5R", 2022); // Å, VÅR
    private static final Course COURSE_A_AUTUMN_2022 = new Course("A", "H\u00D8ST", 2022); // HØST
    private static final Course COURSE_AE_AUTUMN_2022 = new Course("\u00C6", "H\u00D8ST", 2022); // Æ, HØST
    private static final Course COURSE_A_SPRING_2023 = new Course("A", "V\u00C5R", 2023); // VÅR
    private static final Course COURSE_OE_SPRING_2023 = new Course("\u00D8", "V\u00C5R", 2023); // Ø, VÅR
    private static final Course COURSE_AA_SPRING_2023 = new Course("\u00C5", "V\u00C5R", 2023); // Å, VÅR
    final Environment environment = mock(Environment.class);
    private Context context;
    private CoursesByInstitutionOfLoggedInUserHandler handler;
    private ByteArrayOutputStream output;

    @BeforeEach
    public void init(final WireMockRuntimeInfo wmRuntimeInfo) {
        when(environment.readEnv(ApiGatewayHandler.ALLOWED_ORIGIN_ENV)).thenReturn("*");

        final String fsBaseUri = UriWrapper.fromUri(wmRuntimeInfo.getHttpBaseUrl()).toString();
        final String fsConfigString = IoUtils.stringFromResources(Path.of("fsConfig.json"))
                                          .replace("@@BASE_URI@@", fsBaseUri);
        when(environment.readEnvOpt(CoursesByInstitutionOfLoggedInUserHandler.FS_CONFIG_ENV_NAME))
            .thenReturn(Optional.of(fsConfigString));

        context = mock(Context.class);
        output = new ByteArrayOutputStream();
    }

    @Test
    void shouldReturnBadGatewayIfFsIsUnavailable() throws IOException {
        // prepare:
        final InputStream input = createRequest(SUPPORTED_INSTITUTION_PATH);

        final String fsConfigString = IoUtils.stringFromResources(Path.of("fsConfig.json"))
                                          .replace("@@BASE_URI@@", "http://localhost:8081");
        when(environment.readEnvOpt(CoursesByInstitutionOfLoggedInUserHandler.FS_CONFIG_ENV_NAME))
            .thenReturn(Optional.of(fsConfigString));

        var appender = LogUtils.getTestingAppenderForRootLogger();
        this.handler = new CoursesByInstitutionOfLoggedInUserHandler(environment, AFTER_SUMMER);

        // execute:
        handler.handleRequest(input, output, context);

        // verify:
        assertThat(appender.getMessages(), containsString(PROBLEMS_COMMUNICATING_WITH_SERVER_MESSAGE));

        var gatewayResponse = GatewayResponse.fromOutputStream(output, Problem.class);

        assertEquals(HttpURLConnection.HTTP_BAD_GATEWAY, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));

        var problem = gatewayResponse.getBodyObject(Problem.class);
        assertThat(problem.getDetail(), containsString(PROBLEMS_COMMUNICATING_WITH_SERVER_MESSAGE));
    }

    @Test
    void shouldReturnBadGatewayAndLogProblemIfFsReturnsInvalidData() throws IOException {
        // prepare:
        final InputStream input = createRequest(SUPPORTED_INSTITUTION_PATH);

        final String responseBody = "[]";
        stubRequestForCourses(2022, responseBody);

        var appender = LogUtils.getTestingAppenderForRootLogger();

        this.handler = new CoursesByInstitutionOfLoggedInUserHandler(environment, AFTER_SUMMER);

        // execute:
        handler.handleRequest(input, output, context);

        // verify:
        assertThat(appender.getMessages(), containsString(PROBLEMS_READING_RESPONSE_FROM_SERVER_MESSAGE));

        var gatewayResponse = GatewayResponse.fromOutputStream(output, Problem.class);

        assertEquals(HttpURLConnection.HTTP_BAD_GATEWAY, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));

        var problem = gatewayResponse.getBodyObject(Problem.class);
        assertThat(problem.getDetail(), containsString(PROBLEMS_READING_RESPONSE_FROM_SERVER_MESSAGE));
    }

    @Test
    void shouldReturnBadGatewayAndLogProblemIfFsReturnsNonSuccess() throws IOException {
        // prepare:
        final InputStream input = createRequest(SUPPORTED_INSTITUTION_PATH);

        stubFor(get(urlPathEqualTo(TAUGHT_COURSES_URL_PATH))
                    .willReturn(WireMock.aResponse()
                                    .withStatus(HttpURLConnection.HTTP_INTERNAL_ERROR)));

        final TestAppender appender = LogUtils.getTestingAppenderForRootLogger();

        this.handler = new CoursesByInstitutionOfLoggedInUserHandler(environment, AFTER_SUMMER);

        // execute:
        handler.handleRequest(input, output, context);

        // verify:
        final String expectedMessage =
            UNEXPECTED_RESPONSE_CODE_RETURNED_BY_SERVER_MESSAGE + HttpURLConnection.HTTP_INTERNAL_ERROR;
        assertThat(appender.getMessages(), containsString(expectedMessage));

        final var gatewayResponse = GatewayResponse.fromOutputStream(output, Problem.class);

        assertEquals(HttpURLConnection.HTTP_BAD_GATEWAY, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));

        var problem = gatewayResponse.getBodyObject(Problem.class);
        assertThat(problem.getDetail(), containsString(expectedMessage));
    }

    @Test
    void shouldReturnEmptyListOfCoursesWhenInstitutionIsNotConfiguredForFsIntegration() throws IOException {
        // prepare:
        final InputStream input = createRequest(NON_SUPPORTED_INSTITUTION_PATH);

        this.handler = new CoursesByInstitutionOfLoggedInUserHandler(environment, AFTER_SUMMER);

        // execute:
        handler.handleRequest(input, output, context);

        // verify:
        final var gatewayResponse = GatewayResponse.fromOutputStream(output, CoursesResponse.class);

        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));
        assertThat(gatewayResponse.getBodyObject(CoursesResponse.class).getCourses(), emptyIterable());
    }

    @Test
    void shouldReturnEmptyListOfCoursesWhenInstitutionCodeNotResolvable() throws IOException {
        // prepare:
        final InputStream input = new HandlerRequestBuilder<Void>(restApiMapper).build();

        this.handler = new CoursesByInstitutionOfLoggedInUserHandler(environment, AFTER_SUMMER);

        // execute:
        handler.handleRequest(input, output, context);

        // verify:
        final var gatewayResponse = GatewayResponse.fromOutputStream(output, CoursesResponse.class);

        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));
        assertThat(gatewayResponse.getBodyObject(CoursesResponse.class).getCourses(), emptyIterable());
    }

    @Test
    void shouldReturnSortedCoursesTaughtThisFallAndNextSpringWhenCallingAfterStartOfJuly() throws IOException {
        // prepare:
        final InputStream input = createRequest(SUPPORTED_INSTITUTION_PATH);

        stubRequestForCourses(2022, IoUtils.stringFromResources(Path.of("oslometUndervisningResponse2022.json")));
        stubRequestForCourses(2023, IoUtils.stringFromResources(Path.of("oslometUndervisningResponse2023.json")));

        this.handler = new CoursesByInstitutionOfLoggedInUserHandler(environment, AFTER_SUMMER);

        // execute:
        handler.handleRequest(input, output, context);

        // verify:
        final var gatewayResponse = GatewayResponse.fromOutputStream(output, CoursesResponse.class);

        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));

        final var response = gatewayResponse.getBodyObject(CoursesResponse.class);
        assertThat(response.getCourses(), iterableWithSize(5));
        assertThat(response.getCourses(), contains(COURSE_A_AUTUMN_2022,
                                                   COURSE_AE_AUTUMN_2022,
                                                   COURSE_A_SPRING_2023,
                                                   COURSE_OE_SPRING_2023,
                                                   COURSE_AA_SPRING_2023));
    }

    @Test
    void shouldReturnSortedCoursesTaughtThisSpringAndFallWhenCallingBeforeStartOfJuly() throws IOException {
        // prepare:
        final InputStream input = createRequest(SUPPORTED_INSTITUTION_PATH);

        final String responseBody = IoUtils.stringFromResources(Path.of("oslometUndervisningResponse2022.json"));
        stubRequestForCourses(2022, responseBody);

        this.handler = new CoursesByInstitutionOfLoggedInUserHandler(environment, BEFORE_SUMMER);

        // execute:
        handler.handleRequest(input, output, context);

        // verify:
        final var gatewayResponse = GatewayResponse.fromOutputStream(output, CoursesResponse.class);

        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));

        final List<Course> courses = gatewayResponse.getBodyObject(CoursesResponse.class).getCourses();
        assertThat(courses, iterableWithSize(5));
        assertThat(courses, contains(COURSE_B_SPRING_2022,
                                     COURSE_OE_SPRING_2022,
                                     COURSE_AA_SPRING_2022,
                                     COURSE_A_AUTUMN_2022,
                                     COURSE_AE_AUTUMN_2022));
    }

    private InputStream createRequest(String institutionPath)
        throws com.fasterxml.jackson.core.JsonProcessingException {
        final URI topLevelCristinOrgId =
            UriWrapper.fromUri(EXAMPLE_COM_URI).addChild(institutionPath).getUri();
        return new HandlerRequestBuilder<Void>(restApiMapper)
                   .withTopLevelCristinOrgId(topLevelCristinOrgId)
                   .build();
    }

    private void stubRequestForCourses(final int year, final String responseBody) {
        stubFor(get(urlPathEqualTo(TAUGHT_COURSES_URL_PATH))
                    .withHeader(AUTHORIZATION, equalTo(SUCCESS_AUTHORIZATION_HEADER_VALUE))
                    .withQueryParam(YEAR_QUERY_PARAM_NAME, equalTo(Integer.toString(year)))
                    .withQueryParam(DB_ID_QUERY_PARAM_NAME, equalTo("true"))
                    .withQueryParam(INSTITUTION_QUERY_PARAM_NAME, equalTo("215"))
                    .withQueryParam(LIMIT_QUERY_PARAM_NAME, equalTo("0"))
                    .willReturn(WireMock.ok()
                                    .withHeader(CONTENT_TYPE, APPLICATION_JSON_CONTENT_TYPE_VALUE)
                                    .withBody(responseBody)));
    }
}