package edu.kit.datamanager.pit.common;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
public class ExternalServiceException extends IOException {
    public ExternalServiceException(String serviceName) {
        super("Service " + serviceName + " not available.");
    }
}
