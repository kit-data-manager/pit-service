package edu.kit.datamanager.pit.web.doip;

import org.springframework.http.HttpStatus;

import net.dona.doip.DoipConstants;

/**
 * Define valid operations for REST interface of DOIP.
 */
public enum DoipStatus {
    OPERATION_SUCCESS(
        DoipConstants.STATUS_OK,
        HttpStatus.OK,
        "The operation was successfully processed."),
    REQUEST_INVALID(
        DoipConstants.STATUS_BAD_REQUEST,
        HttpStatus.BAD_REQUEST,
        "The request was invalid in some way."),
    AUTHENTICATION_FAIL(
        DoipConstants.STATUS_UNAUTHENTICATED,
        HttpStatus.UNAUTHORIZED,
        "The client did not successfully authenticate."),
    OPERATION_UNAUTHORIZED(
        DoipConstants.STATUS_FORBIDDEN,
        HttpStatus.FORBIDDEN,
        "The client successfully authenticated, but is unauthorized to invoke the operation."),
    DIGITAL_OBJECT_NOT_FOUND(
        DoipConstants.STATUS_NOT_FOUND,
        HttpStatus.NOT_FOUND,
        "The digital object is not known to the service to exist."),
    PID_ALREADY_REGISTERED(
        DoipConstants.STATUS_CONFLICT,
        HttpStatus.CONFLICT,
        "The client tried to create a new digital object with an identifier already in use by an existing digital object."),
    OPERATION_EXECUTION_DECLINED(
        DoipConstants.STATUS_DECLINED,
        HttpStatus.BAD_REQUEST,
        "The service declines to execute the extended operation."),
    OTHER(
        DoipConstants.STATUS_ERROR,
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Error other than the ones stated above occurred.");

    private final String identifier;
    private final HttpStatus httpStatus;
    private final String description;
    
    DoipStatus(String v, HttpStatus http, String description) {
        this.identifier = v;
        this.httpStatus = http;
        this.description = description;
    }
    
    public String getIdentifier() {
        return identifier;
    }
    
    public String getDescription() {
        return description;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
    
    public static DoipStatus fromIdentifier(String v) {
        for (DoipStatus c : DoipStatus.values()) {
            if (c.identifier.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
