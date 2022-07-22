package no.sikt.nva.fs;

import java.net.HttpURLConnection;
import no.sikt.nva.fs.client.HttpException;
import nva.commons.apigateway.exceptions.ApiGatewayException;

public class FailedFsResponseException extends ApiGatewayException {
    public FailedFsResponseException(final HttpException exception) {
        super(exception);
    }

    @Override
    protected Integer statusCode() {
        return HttpURLConnection.HTTP_BAD_GATEWAY;
    }
}
