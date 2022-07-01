package no.sikt.nva.fs;

import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static no.sikt.nva.fs.TestConfig.restApiMapper;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CoursesByInstitutionOfLoggedInUserHandlerTest {

    private Context context;
    private CoursesByInstitutionOfLoggedInUserHandler handler;
    private ByteArrayOutputStream output;

    @BeforeEach
    public void init() {
        var environment = mock(Environment.class);
        when(environment.readEnv(ApiGatewayHandler.ALLOWED_ORIGIN_ENV)).thenReturn("*");

        context = mock(Context.class);

        output = new ByteArrayOutputStream();

        this.handler = new CoursesByInstitutionOfLoggedInUserHandler(environment);
    }

    @AfterEach
    public void tearDown() {

    }

    @Test
    void dummyTest() throws IOException {
        final InputStream input = new HandlerRequestBuilder<Void>(restApiMapper)
                                      .withNvaUsername(randomString())
                                      .withCustomerId(randomUri())
                                      .build();

        handler.handleRequest(input, output, context);

        final GatewayResponse<CoursesByInstitutionResponse> gatewayResponse =
            GatewayResponse.fromOutputStream(output, CoursesByInstitutionResponse.class);

        assertEquals(HttpURLConnection.HTTP_OK, gatewayResponse.getStatusCode());
        assertThat(gatewayResponse.getHeaders(), hasKey(CONTENT_TYPE));
        assertThat(gatewayResponse.getHeaders(), hasKey(ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}