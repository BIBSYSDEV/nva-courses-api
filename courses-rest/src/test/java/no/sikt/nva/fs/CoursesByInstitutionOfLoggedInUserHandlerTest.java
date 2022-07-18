package no.sikt.nva.fs;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static com.google.common.net.HttpHeaders.WWW_AUTHENTICATE;
import static no.sikt.nva.fs.CoursesProvider.LOG_MESSAGE_PREFIX_FS_COMMUNICATION_PROBLEM;
import static no.sikt.nva.fs.TestConfig.restApiMapper;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.ArrayMatching.arrayContaining;
import static org.hamcrest.collection.IsArrayWithSize.arrayWithSize;
import static org.hamcrest.collection.IsArrayWithSize.emptyArray;
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
import java.util.Optional;
import no.sikt.nva.fs.client.HttpUrlConnectionFsClient;
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

@WireMockTest
class CoursesByInstitutionOfLoggedInUserHandlerTest {
    
    public static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2022-08-10T10:15:30.00Z"), TimeProvider.ZONE_ID);
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
    private static final String YEAR_2022 = "2022";
    private static final String YEAR_2023 = "2023";
    private static final Course COURSE_B_SPRING_2022 = new Course("B", "VÅR", 2022);
    private static final Course COURSE_OE_SPRING_2022 = new Course("Ø", "VÅR", 2022);
    private static final Course COURSE_AA_SPRING_2022 = new Course("Å", "VÅR", 2022);
    private static final Course COURSE_A_AUTUMN_2022 = new Course("A", "HØST", 2022);
    private static final Course COURSE_AE_AUTUMN_2022 = new Course("Æ", "HØST", 2022);
    private static final Course COURSE_A_SPRING_2023 = new Course("A", "VÅR", 2023);
    private static final Course COURSE_OE_SPRING_2023 = new Course("Ø", "VÅR", 2023);
    private static final Course COURSE_AA_SPRING_2023 = new Course("Å", "VÅR", 2023);
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
    void shouldReturnEmptyListOfCoursesAndLogProblemIfFsIsUnavailable() throws IOException {
        // prepare:
        var input = createRequest(NON_SUPPORTED_INSTITUTION_PATH);
        this.handler = new CoursesByInstitutionOfLoggedInUserHandler(environment, FIXED_CLOCK);
        
        // execute:
        handler.handleRequest(input, output, context);
        
        // verify:
        var gatewayResponse = GatewayResponse.fromOutputStream(output, Course[].class);
        
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));
        assertThat(gatewayResponse.getBodyObject(Course[].class), emptyArray());
    }
    
    @Test
    void shouldReturnEmptyListOfCoursesAndLogProblemIfFsReturnsNotAuthorized() throws IOException {
        // prepare:
        var input = createRequest(SUPPORTED_INSTITUTION_PATH);
        var appender = LogUtils.getTestingAppenderForRootLogger();
        
        var responseBody = IoUtils.stringFromResources(Path.of("notAuthorizedResponseFromFs.json"));
        stubFor(get(urlPathEqualTo(TAUGHT_COURSES_URL_PATH))
            .willReturn(WireMock.aResponse()
                .withStatus(401)
                .withHeader(WWW_AUTHENTICATE, "Basic realm=\"Unit fs-api\"")
                .withBody(responseBody)));
        
        this.handler = new CoursesByInstitutionOfLoggedInUserHandler(environment, FIXED_CLOCK);
        
        // execute:
        handler.handleRequest(input, output, context);
        
        // verify:
        final GatewayResponse<Course[]> gatewayResponse = GatewayResponse.fromOutputStream(output, Course[].class);
        
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));
        assertThat(gatewayResponse.getBodyObject(Course[].class), emptyArray());
        assertThat(appender.getMessages(), containsString(LOG_MESSAGE_PREFIX_FS_COMMUNICATION_PROBLEM + "215"));
    }
    
    @Test
    void shouldReturnEmptyListOfCoursesAndLogProblemIfFsReturnsNonSuccess() throws IOException {
        // prepare:
        final InputStream input = createRequest(SUPPORTED_INSTITUTION_PATH);
        final TestAppender appender = LogUtils.getTestingAppenderForRootLogger();
        
        stubFor(get(urlPathEqualTo(TAUGHT_COURSES_URL_PATH))
            .willReturn(WireMock.aResponse()
                .withStatus(HttpURLConnection.HTTP_INTERNAL_ERROR)));
        
        this.handler = new CoursesByInstitutionOfLoggedInUserHandler(environment, FIXED_CLOCK);
        
        // execute:
        handler.handleRequest(input, output, context);
        
        // verify:
        final GatewayResponse<Course[]> gatewayResponse = GatewayResponse.fromOutputStream(output, Course[].class);
        
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));
        assertThat(gatewayResponse.getBodyObject(Course[].class), emptyArray());
        assertThat(appender.getMessages(), containsString(
            HttpUrlConnectionFsClient.UNEXPECTED_STATUS_CODE_LOG_MESSAGE_PREFIX + "500"));
    }
    
    @Test
    void shouldReturnEmptyListOfCoursesWhenInstitutionDoesNotHaveFsAccess() throws IOException {
        // prepare:
        final InputStream input = new HandlerRequestBuilder<Void>(restApiMapper)
            .build();
        
        this.handler = new CoursesByInstitutionOfLoggedInUserHandler(environment, FIXED_CLOCK);
        
        // execute:
        handler.handleRequest(input, output, context);
        
        // verify:
        final GatewayResponse<Course[]> gatewayResponse = GatewayResponse.fromOutputStream(output, Course[].class);
        
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));
        assertThat(gatewayResponse.getBodyObject(Course[].class), emptyArray());
    }
    
    @Test
    void shouldReturnCoursesWhenInstitutionHasFsAccessAfterSummer() throws IOException {
        // prepare:
        final InputStream input = createRequest(SUPPORTED_INSTITUTION_PATH);
        
        final String responseBody2022 = IoUtils.stringFromResources(Path.of("oslometUndervisningResponse2022.json"));
        stubFor(get(urlPathEqualTo(TAUGHT_COURSES_URL_PATH))
            .withHeader(AUTHORIZATION, equalTo(SUCCESS_AUTHORIZATION_HEADER_VALUE))
            .withQueryParam(YEAR_QUERY_PARAM_NAME, equalTo(YEAR_2022))
            .withQueryParam(DB_ID_QUERY_PARAM_NAME, equalTo("true"))
            .withQueryParam(INSTITUTION_QUERY_PARAM_NAME, equalTo("215"))
            .withQueryParam(LIMIT_QUERY_PARAM_NAME, equalTo("0"))
            .willReturn(WireMock.ok()
                .withHeader(CONTENT_TYPE, APPLICATION_JSON_CONTENT_TYPE_VALUE)
                .withBody(responseBody2022)));
        
        final String responseBody2023 = IoUtils.stringFromResources(Path.of("oslometUndervisningResponse2023.json"));
        stubFor(get(urlPathEqualTo(TAUGHT_COURSES_URL_PATH))
            .withHeader(AUTHORIZATION, equalTo(SUCCESS_AUTHORIZATION_HEADER_VALUE))
            .withQueryParam(YEAR_QUERY_PARAM_NAME, equalTo(YEAR_2023))
            .withQueryParam(DB_ID_QUERY_PARAM_NAME, equalTo("true"))
            .withQueryParam(INSTITUTION_QUERY_PARAM_NAME, equalTo("215"))
            .withQueryParam(LIMIT_QUERY_PARAM_NAME, equalTo("0"))
            .willReturn(WireMock.ok()
                .withHeader(CONTENT_TYPE, APPLICATION_JSON_CONTENT_TYPE_VALUE)
                .withBody(responseBody2023)));
        
        this.handler = new CoursesByInstitutionOfLoggedInUserHandler(environment, FIXED_CLOCK);
        
        // execute:
        handler.handleRequest(input, output, context);
        
        // verify:
        final GatewayResponse<Course[]> gatewayResponse = GatewayResponse.fromOutputStream(output, Course[].class);
        
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));
        
        final Course[] courses = gatewayResponse.getBodyObject(Course[].class);
        assertThat(courses, arrayWithSize(5));
        assertThat(courses, arrayContaining(COURSE_A_AUTUMN_2022,
            COURSE_AE_AUTUMN_2022,
            COURSE_A_SPRING_2023,
            COURSE_OE_SPRING_2023,
            COURSE_AA_SPRING_2023));
    }
    
    @Test
    void shouldReturnCoursesWhenInstitutionHasFsAccessBeforeSummer() throws IOException {
        // prepare:
        final InputStream input = createRequest(SUPPORTED_INSTITUTION_PATH);
        
        final String responseBody2022 = IoUtils.stringFromResources(Path.of("oslometUndervisningResponse2022.json"));
        stubFor(get(urlPathEqualTo(TAUGHT_COURSES_URL_PATH))
            .withHeader(AUTHORIZATION, equalTo(SUCCESS_AUTHORIZATION_HEADER_VALUE))
            .withQueryParam(YEAR_QUERY_PARAM_NAME, equalTo(YEAR_2022))
            .withQueryParam(DB_ID_QUERY_PARAM_NAME, equalTo("true"))
            .withQueryParam(INSTITUTION_QUERY_PARAM_NAME, equalTo("215"))
            .withQueryParam(LIMIT_QUERY_PARAM_NAME, equalTo("0"))
            .willReturn(WireMock.ok()
                .withHeader(CONTENT_TYPE, APPLICATION_JSON_CONTENT_TYPE_VALUE)
                .withBody(responseBody2022)));
        
        this.handler = new CoursesByInstitutionOfLoggedInUserHandler(environment, BEFORE_SUMMER);
        
        // execute:
        handler.handleRequest(input, output, context);
        
        // verify:
        final GatewayResponse<Course[]> gatewayResponse = GatewayResponse.fromOutputStream(output, Course[].class);
        
        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));
        
        final Course[] courses = gatewayResponse.getBodyObject(Course[].class);
        assertThat(courses, arrayWithSize(5));
        assertThat(courses, arrayContaining(COURSE_B_SPRING_2022,
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
}