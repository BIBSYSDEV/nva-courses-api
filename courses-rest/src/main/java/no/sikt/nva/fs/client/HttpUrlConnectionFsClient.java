package no.sikt.nva.fs.client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Base64;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.paths.UriWrapper;

public class HttpUrlConnectionFsClient implements FsClient {

    private static final String TEACHING_URI_PATH = "undervisning";
    private static final String DB_IDENTIFIER_QUERY_PARAM_NAME = "dbId";
    private static final String INSTITUTION_QUERY_PARAM_NAME = "emne.institusjon";
    private static final String YEAR_QUERY_PARAM_NAME = "semester.ar";
    private static final String LIMIT_QUERY_PARAM_NAME = "limit";
    private static final String UNLIMITED = "0";
    public static final String PROBLEMS_READING_RESPONSE_FROM_SERVER_MESSAGE = "Problems reading response from server";
    public static final String PROBLEMS_COMMUNICATING_WITH_SERVER_MESSAGE = "Problems communicating with server";
    private final HttpClient httpClient;
    private final String baseUri;
    private final int institutionCode;
    private final String username;
    private final String password;

    public HttpUrlConnectionFsClient(final String baseUri, final int institutionCode, final String username,
                                     final String password) {

        this.httpClient = HttpClient.newBuilder().build();
        this.baseUri = baseUri;
        this.institutionCode = institutionCode;
        this.username = username;
        this.password = password;
    }

    @Override
    public FsCollectionResponse getTaughtCourses(final int year) {
        final URI uri = UriWrapper.fromUri(baseUri)
                            .addChild(TEACHING_URI_PATH)
                            .addQueryParameter(DB_IDENTIFIER_QUERY_PARAM_NAME, "true")
                            .addQueryParameter(INSTITUTION_QUERY_PARAM_NAME, Integer.toString(institutionCode))
                            .addQueryParameter(YEAR_QUERY_PARAM_NAME, Integer.toString(year))
                            .addQueryParameter(LIMIT_QUERY_PARAM_NAME, UNLIMITED)
                            .getUri();

        final HttpRequest request = HttpRequest.newBuilder(uri)
                                        .GET()
                                        .setHeader("Authorization", getBasicAuthenticationHeader())
                                        .build();
        final HttpResponse<byte[]> response = sendRequest(request);
        final int statusCode = response.statusCode();
        if (statusCode == HttpURLConnection.HTTP_OK) {
            return convertResponseBody(response.body());
        } else {
            throw new HttpStatusException(statusCode);
        }
    }

    private FsCollectionResponse convertResponseBody(byte[] body) {
        try {
            return JsonUtils.dtoObjectMapper.readValue(body, FsCollectionResponse.class);
        } catch (IOException e) {
            throw new HttpException(PROBLEMS_READING_RESPONSE_FROM_SERVER_MESSAGE, e);
        }
    }

    private HttpResponse<byte[]> sendRequest(HttpRequest request) {
        try {
            return httpClient.send(request, BodyHandlers.ofByteArray());
        } catch (IOException | InterruptedException e) {
            throw new HttpException(PROBLEMS_COMMUNICATING_WITH_SERVER_MESSAGE, e);
        }
    }

    private String getBasicAuthenticationHeader() {
        final String valueToEncode = this.username + ":" + this.password;
        return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
    }
}
