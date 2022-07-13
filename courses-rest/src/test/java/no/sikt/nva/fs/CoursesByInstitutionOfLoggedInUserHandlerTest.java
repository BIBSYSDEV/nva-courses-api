package no.sikt.nva.fs;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static no.sikt.nva.fs.CoursesProvider.LOG_MESSAGE_PREFIX_FS_COMMUNICATION_PROBLEM;
import static no.sikt.nva.fs.TestConfig.restApiMapper;
import static org.hamcrest.MatcherAssert.assertThat;
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
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import nva.commons.core.ioutils.IoUtils;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogUtils;
import nva.commons.logutils.TestAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@WireMockTest
class CoursesByInstitutionOfLoggedInUserHandlerTest {

    private static final String NON_SUPPORTED_INSTITUTION_PATH = "100.0.0.0.0";
    private static final String SUPPORTED_INSTITUTION_PATH = "215.0.0.0.0";
    private Context context;
    private CoursesByInstitutionOfLoggedInUserHandler handler;
    private ByteArrayOutputStream output;
    final Environment environment = mock(Environment.class);

    @BeforeEach
    public void init(final WireMockRuntimeInfo wmRuntimeInfo) {
        when(environment.readEnv(ApiGatewayHandler.ALLOWED_ORIGIN_ENV)).thenReturn("*");

        final String fsBaseUri = UriWrapper.fromUri(wmRuntimeInfo.getHttpBaseUrl()).toString();
        final String fsConfigString = IoUtils.stringFromResources(
            Path.of("fsConfig.json")).replace("@@BASE_URI@@", fsBaseUri);
        when(environment.readEnv(CoursesByInstitutionOfLoggedInUserHandler.FS_CONFIG_ENV_NAME))
            .thenReturn(fsConfigString);

        context = mock(Context.class);
        output = new ByteArrayOutputStream();
    }

    @AfterEach
    public void tearDown() {

    }

    @Test
    void shouldReturnEmptyListOfCoursesAndLogProblemIfFsIsUnavailable() throws IOException {
        // prepare:
        final URI topLevelCristinOrgId =
            UriWrapper.fromUri("http://example.com").addChild(NON_SUPPORTED_INSTITUTION_PATH).getUri();
        final InputStream input = new HandlerRequestBuilder<Void>(restApiMapper)
                                      .withTopLevelCristinOrgId(topLevelCristinOrgId)
                                      .build();

        final TimeProvider timeProvider = new FixedTimeProvider(2022, 1, 1);

        this.handler = new CoursesByInstitutionOfLoggedInUserHandler(environment, timeProvider);

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
    void shouldReturnEmptyListOfCoursesAndLogProblemIfFsReturnsNotAuthorized() throws IOException {
        // prepare:
        final URI topLevelCristinOrgId =
            UriWrapper.fromUri("http://example.com").addChild(SUPPORTED_INSTITUTION_PATH).getUri();
        final InputStream input = new HandlerRequestBuilder<Void>(restApiMapper)
                                      .withTopLevelCristinOrgId(topLevelCristinOrgId)
                                      .build();
        final TestAppender appender = LogUtils.getTestingAppenderForRootLogger();

        String responseBody = IoUtils.stringFromResources(Path.of("notAuthorizedResponseFromFs.json"));
        stubFor(get(urlPathEqualTo("/undervisning"))
                    .willReturn(WireMock.aResponse()
                                    .withStatus(401)
                                    .withHeader("WWW-Authenticate", "Basic realm=\"Unit fs-api\"")
                                    .withBody(responseBody)));

        final TimeProvider timeProvider = new FixedTimeProvider(2022, 8, 10);

        this.handler = new CoursesByInstitutionOfLoggedInUserHandler(environment, timeProvider);

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
    void shouldReturnEmptyListOfCoursesWhenInstitutionDoesNotHaveFsAccess() throws IOException {
        // prepare:
        final InputStream input = new HandlerRequestBuilder<Void>(restApiMapper)
                                      .build();

        final TimeProvider timeProvider = new FixedTimeProvider(2022, 8, 10);

        this.handler = new CoursesByInstitutionOfLoggedInUserHandler(environment, timeProvider);

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
        final URI topLevelCristinOrgId =
            UriWrapper.fromUri("http://example.com").addChild(SUPPORTED_INSTITUTION_PATH).getUri();
        final InputStream input = new HandlerRequestBuilder<Void>(restApiMapper)
                                      .withTopLevelCristinOrgId(topLevelCristinOrgId)
                                      .build();

        final String responseBody2022 = IoUtils.stringFromResources(Path.of("oslometUndervisningResponse2022.json"));
        stubFor(get(urlPathEqualTo("/undervisning"))
                    .withQueryParam("semester.ar", equalTo("2022"))
                    .willReturn(WireMock.ok()
                                    .withHeader("Content-Type", "application/json")
                                    .withBody(responseBody2022)));

        final String responseBody2023 = IoUtils.stringFromResources(Path.of("oslometUndervisningResponse2023.json"));
        stubFor(get(urlPathEqualTo("/undervisning"))
                    .withQueryParam("semester.ar", equalTo("2023"))
                    .willReturn(WireMock.ok()
                                    .withHeader("Content-Type", "application/json")
                                    .withBody(responseBody2023)));

        final TimeProvider timeProvider = new FixedTimeProvider(2022, 8, 10);

        this.handler = new CoursesByInstitutionOfLoggedInUserHandler(environment, timeProvider);

        // execute:
        handler.handleRequest(input, output, context);

        // verify:
        final GatewayResponse<Course[]> gatewayResponse = GatewayResponse.fromOutputStream(output, Course[].class);

        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));

        final Course[] courses = gatewayResponse.getBodyObject(Course[].class);
        assertThat(courses, arrayWithSize(1117));
    }

    @Test
    void shouldReturnCoursesWhenInstitutionHasFsAccessBeforeSummer() throws IOException {
        // prepare:
        final URI topLevelCristinOrgId =
            UriWrapper.fromUri("http://example.com").addChild(SUPPORTED_INSTITUTION_PATH).getUri();
        final InputStream input = new HandlerRequestBuilder<Void>(restApiMapper)
                                      .withTopLevelCristinOrgId(topLevelCristinOrgId)
                                      .build();

        final String responseBody2022 = IoUtils.stringFromResources(Path.of("oslometUndervisningResponse2022.json"));
        stubFor(get(urlPathEqualTo("/undervisning"))
                    .withQueryParam("semester.ar", equalTo("2022"))
                    .willReturn(WireMock.ok()
                                    .withHeader("Content-Type", "application/json")
                                    .withBody(responseBody2022)));

        final TimeProvider timeProvider = new FixedTimeProvider(2022, 3, 10);

        this.handler = new CoursesByInstitutionOfLoggedInUserHandler(environment, timeProvider);

        // execute:
        handler.handleRequest(input, output, context);

        // verify:
        final GatewayResponse<Course[]> gatewayResponse = GatewayResponse.fromOutputStream(output, Course[].class);

        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));

        final Course[] courses = gatewayResponse.getBodyObject(Course[].class);
        assertThat(courses, arrayWithSize(2090));
    }
}