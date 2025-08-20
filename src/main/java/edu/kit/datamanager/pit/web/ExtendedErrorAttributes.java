/*
 * Copyright (c) 2025 Karlsruhe Institute of Technology.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.kit.datamanager.pit.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.datamanager.pit.common.RecordValidationException;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

@Component
public class ExtendedErrorAttributes extends DefaultErrorAttributes {

    private final ObjectMapper objectMapperBean;

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