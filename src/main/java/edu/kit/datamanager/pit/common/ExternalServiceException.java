package edu.kit.datamanager.pit.common;


import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ExternalServiceException extends ResponseStatusException {

    private static final String MESSAGE_TERM_SERVICE = "Service";
    private static final long serialVersionUID = 1L;
    private static final HttpStatus HTTP_STATUS = HttpStatus.SERVICE_UNAVAILABLE;

    public ExternalServiceException(String serviceName) {
        super(HTTP_STATUS, "Service " + serviceName + " not available.");
    }

    public ExternalServiceException(String serviceName, Throwable e) {
        super(HTTP_STATUS, MESSAGE_TERM_SERVICE + serviceName + "returned an error", e);
    }

    public ExternalServiceException(String serviceName, String message) {
        super(HTTP_STATUS, MESSAGE_TERM_SERVICE + serviceName + ": " + message);
    }

    public ExternalServiceException(String serviceName, String message, Throwable e) {
        super(HTTP_STATUS, MESSAGE_TERM_SERVICE + serviceName + ": " + message, e);
    }
}
