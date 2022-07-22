package no.sikt.nva.fs.client;

public class HttpException extends RuntimeException {

    public HttpException(final String message) {
        super(message);
    }

    public HttpException(String message, Throwable cause) {
        super(message, cause);
    }
}
