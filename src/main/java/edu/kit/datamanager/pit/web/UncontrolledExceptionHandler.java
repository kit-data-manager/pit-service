package edu.kit.datamanager.pit.web;

import java.util.Map;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.error.ErrorAttributeOptions.Include;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Handles Exceptions which are not already resolved properly to according error
 * responses for the REST interface.
 * 
 * This is usually the case for exceptions we can not control. Like Exceptions
 * we did not create, especially in generated code (databases!).
 */
@ControllerAdvice
public class UncontrolledExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String BODY_STATUS = "status";
    private static final String BODY_ERROR = "error";
    private static final String BODY_MESSAGE = "message";

    @ExceptionHandler( value = { DataIntegrityViolationException.class })
    protected ResponseEntity<Object> databaseErrors(RuntimeException ex, WebRequest request) {
        // What we would like to return in this case:
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        int statusCode = status.value();
        String statusPhrase = HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase();
        String errorMessage = "Database error. " + ex.toString();

        // Getting springs default json response for HTTP response body on error.
        // Filling it then manually with what we would like to return.
        Map<String, Object> body = new DefaultErrorAttributes()
                .getErrorAttributes(request, getErrorAttributeOptions());
        body.put(BODY_STATUS, statusCode);
        body.put(BODY_ERROR, statusPhrase);
        body.put(BODY_MESSAGE, errorMessage);

        return handleExceptionInternal(
            ex,
            body, // body
            new HttpHeaders(),
            HttpStatus.INTERNAL_SERVER_ERROR,
            request
        );
    }
    
    private ErrorAttributeOptions getErrorAttributeOptions() {
        ErrorAttributeOptions options = ErrorAttributeOptions.defaults();
        options = options.including(Include.MESSAGE);
        options = options.including(Include.BINDING_ERRORS);
        return options;
    }
}
