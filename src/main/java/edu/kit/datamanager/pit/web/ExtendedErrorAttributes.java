package edu.kit.datamanager.pit.web;

import java.util.Map;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.web.context.request.WebRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.kit.datamanager.pit.common.RecordValidationException;

public class ExtendedErrorAttributes extends DefaultErrorAttributes {

    ObjectMapper objectMapperBean;

    public ExtendedErrorAttributes(ObjectMapper objectMapperBean) {
        this.objectMapperBean = objectMapperBean;
    }

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        final Map<String, Object> errorAttributes = 
            super.getErrorAttributes(webRequest, options);

        final Throwable error = super.getError(webRequest);
        if (error instanceof RecordValidationException validationError) {
            try {
                errorAttributes.put("pid-record", objectMapperBean.writeValueAsString(validationError.getPidRecord()));
            } catch (Exception e) {
                // just to make sure
            }
        }

        return errorAttributes;
    }
}