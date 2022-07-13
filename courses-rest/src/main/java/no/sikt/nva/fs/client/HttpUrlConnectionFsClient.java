package no.sikt.nva.fs.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Base64;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpUrlConnectionFsClient implements FsClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpUrlConnectionFsClient.class);
    private static final String TEACHING_URI_PATH = "undervisning";
    private static final String INSTITUTION_QUERY_PARAM_NAME = "emne.institusjon";
    private static final String YEAR_QUERY_PARAM_NAME = "semester.ar";
    private static final String LIMIT_QUERY_PARAM_NAME = "limit";
    private static final String UNLIMITED = "0";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUri;
    private final int institutionCode;
    private final String username;
    private final String password;

    public HttpUrlConnectionFsClient(final ObjectMapper objectMapper, final String baseUri,
                                     final int institutionCode, final String username,
                                     final String password) {

        this.httpClient = HttpClient.newBuilder()
                              .authenticator(new Authenticator() {
                                  @Override
                                  protected PasswordAuthentication getPasswordAuthentication() {
                                      return new PasswordAuthentication(username, password.toCharArray());
                                  }
                              })
                              .build();
        this.objectMapper = objectMapper;
        this.baseUri = baseUri;
        this.institutionCode = institutionCode;
        this.username = username;
        this.password = password;
    }

    @Override
    public FsCollectionResponse getTaughtCourses(final int year) {
        final URI uri = UriWrapper.fromUri(baseUri)
                            .addChild(TEACHING_URI_PATH)
                            .addQueryParameter(INSTITUTION_QUERY_PARAM_NAME, Integer.toString(institutionCode))
                            .addQueryParameter(YEAR_QUERY_PARAM_NAME, Integer.toString(year))
                            .addQueryParameter(LIMIT_QUERY_PARAM_NAME, UNLIMITED)
                            .getUri();

        final HttpRequest request = HttpRequest.newBuilder(uri)
                                        .GET()
                                        .setHeader("Authorization", getBasicAuthenticationHeader())
                                        .build();
        try {
            final HttpResponse<byte[]> response = httpClient.send(request, BodyHandlers.ofByteArray());
            if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                return objectMapper.readValue(response.body(), FsCollectionResponse.class);
            } else {
                final String message = String.format("Unexpected response code from FS: %d", response.statusCode());
                LOGGER.error(message);
                throw new RuntimeException(message);
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Unable to communicate with FS", e);
            throw new RuntimeException(e);
        }
    }

    private String getBasicAuthenticationHeader() {
        final String valueToEncode = this.username + ":" + this.password;
        return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
    }
}
