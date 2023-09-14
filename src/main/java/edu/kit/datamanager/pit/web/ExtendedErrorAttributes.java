package edu.kit.datamanager.pit.web;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.kit.datamanager.pit.common.RecordValidationException;

@Component
public class ExtendedErrorAttributes extends DefaultErrorAttributes {

    @Autowired(required = true)
    ObjectMapper objectMapperBean;

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        final Map<String, Object> errorAttributes = 
            super.getErrorAttributes(webRequest, options);

        final Throwable error = super.getError(webRequest);
        if (error instanceof RecordValidationException) {
            final RecordValidationException validationError = (RecordValidationException) error;
            try {
                errorAttributes.put("pid-record", objectMapperBean.writeValueAsString(validationError.getPidRecord()));
            } catch (Exception e) {
                // just to make sure
            }
        }

        return errorAttributes;
    }
}