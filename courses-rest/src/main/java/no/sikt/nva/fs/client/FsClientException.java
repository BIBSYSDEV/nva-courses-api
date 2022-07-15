package no.sikt.nva.fs.client;

public class FsClientException extends RuntimeException {

    public FsClientException(String message) {
        super(message);
    }

    public FsClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
