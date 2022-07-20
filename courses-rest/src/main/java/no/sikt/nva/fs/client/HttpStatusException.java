package no.sikt.nva.fs.client;

public class HttpStatusException extends HttpException {

    public static final String UNEXPECTED_RESPONSE_CODE_RETURNED_BY_SERVER_MESSAGE = "Unexpected response code "
                                                                                    + "returned by sever: ";

    public HttpStatusException(int responseCode) {
        super(UNEXPECTED_RESPONSE_CODE_RETURNED_BY_SERVER_MESSAGE + responseCode);
    }
}
