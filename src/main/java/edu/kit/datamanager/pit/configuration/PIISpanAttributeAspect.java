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
package edu.kit.datamanager.pit.configuration;

import io.opentelemetry.api.trace.Span;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Aspect-Oriented Programming (AOP) aspect that automatically intercepts method calls
 * within the PIT service to extract and add Personally Identifiable Information (PII)
 * data to OpenTelemetry distributed tracing spans.
 *
 * <p>This aspect provides enhanced observability for development and debugging purposes
 * by capturing sensitive parameter values that are explicitly marked with the
 * {@link PIISpanAttribute} annotation.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Conditional Activation:</strong> Only created when the configuration property
 *       {@code pit.observability.includePiiInTraces=true} is set</li>
 *   <li><strong>Broad Interception:</strong> Intercepts ALL methods in the
 *       {@code edu.kit.datamanager.pit} package tree</li>
 *   <li><strong>Selective Processing:</strong> Only processes methods that have parameters
 *       annotated with {@link PIISpanAttribute}</li>
 *   <li><strong>OpenTelemetry Integration:</strong> Seamlessly integrates with the existing
 *       tracing infrastructure</li>
 * </ul>
 *
 * <h3>Security and Privacy Considerations:</h3>
 * <p><strong>⚠️ CRITICAL SECURITY WARNING:</strong> This aspect captures and exports
 * potentially sensitive PII data to tracing systems. This functionality should
 * <strong>NEVER</strong> be enabled in production environments and should be used
 * with extreme caution in development environments.</p>
 *
 * <ul>
 *   <li>PII data may include user IDs, email addresses, personal identifiers, etc.</li>
 *   <li>Traced data may be stored in external observability platforms</li>
 *   <li>Ensure compliance with privacy regulations (GDPR, CCPA, etc.)</li>
 *   <li>Consider data retention policies and access controls</li>
 * </ul>
 *
 * <h3>Performance Considerations:</h3>
 * <ul>
 *   <li>Intercepts ALL method calls in the pit package (performance overhead)</li>
 *   <li>Uses reflection to inspect method parameters (additional CPU cost)</li>
 *   <li>Should be disabled in performance-critical production environments</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>
 * {@code
 * @Service
 * public class UserService {
 *     public User findUser(@PIISpanAttribute("userId") String userId) {
 *         // This method call will be intercepted and userId will be added to the span
 *         return userRepository.findById(userId);
 *     }
 * }
 * }
 * </pre>
 *
 * @see PIISpanAttribute
 * @see io.opentelemetry.api.trace.Span
 */
@Aspect
@Component
@ConditionalOnProperty(name = "pit.observability.includePiiInTraces", havingValue = "true")
public class PIISpanAttributeAspect {

    private static final Logger LOG = LoggerFactory.getLogger(PIISpanAttributeAspect.class);

    /**
     * Spring's parameter name discoverer used to retrieve parameter names from method signatures.
     * This is essential when the {@link PIISpanAttribute} annotation doesn't specify a custom
     * attribute name - we fall back to using the actual parameter name from the source code.
     *
     * <p>The DefaultParameterNameDiscoverer tries multiple strategies:
     * <ul>
     *   <li>Uses debug information if available (compiled with -g flag)</li>
     *   <li>Falls back to ASM-based bytecode analysis</li>
     *   <li>Uses Java 8+ parameter names if compiled with -parameters flag</li>
     * </ul>
     */
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    /**
     * Constructor that logs the activation of PII tracing with appropriate warnings.
     *
     * <p>This constructor is only called when the Spring condition
     * {@code pit.observability.includePiiInTraces=true} is met, ensuring that
     * the aspect is only active when explicitly configured.</p>
     *
     * <p>The constructor logs both informational and warning messages to ensure
     * that the activation of PII tracing is clearly visible in application logs,
     * helping to prevent accidental deployment to production environments.</p>
     */
    public PIISpanAttributeAspect() {
        LOG.info("PIISpanAttributeAspect created - PII data will be included in traces");
        LOG.warn("WARNING: PII tracing is enabled! This should only be used in development environments.");
        LOG.info("Aspect will intercept methods in package: edu.kit.datamanager.pit.*");
        LOG.info("Only methods with @PIISpanAttribute annotated parameters will have PII data extracted");
    }

    /**
     * AspectJ around advice that intercepts ALL method calls within the PIT service
     * package hierarchy to identify and process methods containing PII parameters.
     *
     * <p>This method uses a broad pointcut expression that matches every method execution
     * in the {@code edu.kit.datamanager.pit} package and all its sub-packages. While this
     * approach has performance implications, it ensures comprehensive coverage without
     * requiring developers to explicitly mark classes or methods.</p>
     *
     * <h3>Execution Flow:</h3>
     * <ol>
     *   <li>Intercept method call before execution</li>
     *   <li>Perform quick scan of method parameters for {@link PIISpanAttribute} annotations</li>
     *   <li>If PII parameters found, extract and add them to the current OpenTelemetry span</li>
     *   <li>Proceed with original method execution</li>
     *   <li>Handle any errors gracefully without disrupting the original method</li>
     * </ol>
     *
     * <h3>Performance Optimization:</h3>
     * <p>To minimize performance impact, this method performs a quick preliminary check
     * for PII annotations before proceeding with the more expensive span processing.
     * Methods without PII parameters are processed with minimal overhead.</p>
     *
     * <h3>Error Handling:</h3>
     * <p>Any exceptions during PII processing are caught and logged but do not interfere
     * with the original method execution. This ensures that tracing issues don't break
     * application functionality.</p>
     *
     * @param joinPoint the AspectJ join point containing method signature and arguments
     * @return the result of the original method execution
     * @throws Throwable any exception thrown by the original method (PII processing exceptions are caught)
     */
    @Around("execution(* edu.kit.datamanager.pit..*(..))")
    public Object interceptAllMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        // Extract method information using AspectJ reflection capabilities
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();

        // Performance optimization: Quick scan for PII annotations before expensive processing
        // This avoids the overhead of span processing for methods that don't have PII data
        boolean hasPIIParams = false;
        for (Parameter parameter : parameters) {
            if (parameter.getAnnotation(PIISpanAttribute.class) != null) {
                hasPIIParams = true;
                break; // Early exit once we find the first PII parameter
            }
        }

        // Only process methods that actually have PII parameters
        if (hasPIIParams) {
            LOG.info("Found method with PII parameters: {}.{}",
                    method.getDeclaringClass().getSimpleName(), method.getName());

            try {
                LOG.info("Processing PII parameters for tracing");
                // Delegate to specialized method for span attribute processing
                addPIIAttributesToCurrentSpan(joinPoint);
            } catch (Exception e) {
                // Critical: Ensure that PII processing errors don't break the application
                // Log the error but continue with normal method execution
                LOG.warn("Failed to add PII span attributes: {}", e.getMessage(), e);
            }
        }

        // Always proceed with the original method execution
        // This is the core of the around advice - we must call proceed() to execute the original method
        return joinPoint.proceed();
    }

    /**
     * Core processing method that extracts PII data from method parameters and adds
     * them as attributes to the current OpenTelemetry span.
     *
     * <p>This method performs the detailed work of:
     * <ul>
     *   <li>Validating that a valid OpenTelemetry span context exists</li>
     *   <li>Iterating through method parameters to find PII annotations</li>
     *   <li>Extracting parameter values and converting them to string representations</li>
     *   <li>Adding the PII data as span attributes for observability</li>
     * </ul>
     *
     * <h3>OpenTelemetry Integration:</h3>
     * <p>This method relies on OpenTelemetry's automatic span propagation through
     * thread-local storage. The {@code Span.current()} call retrieves the active
     * span from the current thread's context, which should have been created by
     * OpenTelemetry's auto-instrumentation or manual span creation.</p>
     *
     * <h3>Parameter Name Resolution:</h3>
     * <p>The method uses Spring's {@link ParameterNameDiscoverer} to resolve parameter
     * names when the annotation doesn't specify a custom attribute name. This requires
     * that the application be compiled with parameter name information (Java 8+ with
     * -parameters flag or debug information with -g flag).</p>
     *
     * <h3>Data Safety:</h3>
     * <ul>
     *   <li>Null parameter values are safely handled and logged</li>
     *   <li>Very long parameter values are truncated in log messages (but not in spans)</li>
     *   <li>All parameter values are converted to strings using {@code toString()}</li>
     * </ul>
     *
     * @param joinPoint the AspectJ join point containing method signature and runtime arguments
     */
    private void addPIIAttributesToCurrentSpan(ProceedingJoinPoint joinPoint) {
        // Extract all necessary method information from the join point
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs(); // Runtime argument values
        Parameter[] parameters = method.getParameters(); // Method parameter definitions

        // Attempt to discover parameter names for cases where annotation doesn't specify attribute name
        // This uses reflection and bytecode analysis to retrieve the original parameter names
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);

        // Retrieve the current OpenTelemetry span from thread-local context
        // This span should have been created by OpenTelemetry's auto-instrumentation
        Span currentSpan = Span.current();
        if (currentSpan == null) {
            LOG.warn("No current span available for PII attributes - OpenTelemetry may not be properly configured");
            return;
        }

        // Validate that the span context is properly initialized and active
        // An invalid span context indicates tracing infrastructure issues
        if (!currentSpan.getSpanContext().isValid()) {
            LOG.warn("Current span context is not valid - span may have been closed or not properly created");
            return;
        }

        LOG.info("Current span: {}", currentSpan.getSpanContext().getSpanId());

        // Process each parameter to look for PII annotations
        int piiParamsProcessed = 0;
        for (int i = 0; i < parameters.length && i < args.length; i++) {
            Parameter parameter = parameters[i];
            PIISpanAttribute piiAnnotation = parameter.getAnnotation(PIISpanAttribute.class);

            if (piiAnnotation != null) {
                if (args[i] != null) {
                    // Determine the span attribute name (from annotation or parameter name)
                    String attributeName = determineAttributeName(piiAnnotation, parameterNames, i);

                    // Convert parameter value to string representation
                    // Note: This uses toString() which may not be suitable for all object types
                    String attributeValue = args[i].toString();

                    // Add the PII data to the OpenTelemetry span as a custom attribute
                    // This data will be included in distributed traces and exported to observability platforms
                    currentSpan.setAttribute(attributeName, attributeValue);
                    piiParamsProcessed++;

                    // Log the addition with truncation for very long values to avoid log spam
                    LOG.debug("Successfully added PII span attribute: {} = {}", attributeName,
                            attributeValue.length() > 100 ? attributeValue.substring(0, 100) + "..." : attributeValue);
                } else {
                    // Handle null parameter values gracefully - log but don't add to span
                    LOG.debug("PII parameter at index {} is null, skipping", i);
                }
            }
        }

        // Summary logging to track PII processing activity
        LOG.info("Total PII parameters processed: {} for method {}.{}", piiParamsProcessed,
                method.getDeclaringClass().getSimpleName(), method.getName());
    }

    /**
     * Determines the most appropriate span attribute name for a PII parameter using
     * a fallback strategy that prioritizes explicit annotation values, then parameter
     * names, and finally generates a generic name.
     *
     * <p>This method implements a three-tier naming strategy:</p>
     * <ol>
     *   <li><strong>Explicit Annotation Value:</strong> If the {@link PIISpanAttribute}
     *       annotation specifies a non-empty value, use it as the attribute name.
     *       This gives developers full control over span attribute naming.</li>
     *   <li><strong>Parameter Name Discovery:</strong> If no explicit value is provided,
     *       attempt to use the actual parameter name from the method signature.
     *       This requires proper compilation settings to preserve parameter names.</li>
     *   <li><strong>Generated Fallback:</strong> If parameter names are not available,
     *       generate a generic attribute name based on the parameter position.</li>
     * </ol>
     *
     * <h3>Compilation Requirements for Parameter Names:</h3>
     * <p>For the second tier to work properly, the application must be compiled with
     * one of the following options:</p>
     * <ul>
     *   <li><strong>Java 8+ with -parameters flag:</strong> Preserves parameter names in bytecode</li>
     *   <li><strong>Debug information (-g flag):</strong> Includes variable names in debug info</li>
     *   <li><strong>IDE default settings:</strong> Most IDEs enable parameter name preservation by default</li>
     * </ul>
     *
     * <h3>Attribute Naming Best Practices:</h3>
     * <ul>
     *   <li>Use descriptive, consistent names for span attributes</li>
     *   <li>Consider namespace prefixes for application-specific attributes (e.g., "app.user.id")</li>
     *   <li>Avoid special characters that may cause issues in observability platforms</li>
     *   <li>Keep names reasonably short to minimize storage overhead</li>
     * </ul>
     *
     * @param annotation     the PII span attribute annotation that may contain an explicit name
     * @param parameterNames array of parameter names discovered from the method signature, may be null
     * @param parameterIndex zero-based index of the parameter in the method signature
     * @return the determined span attribute name, never null or empty
     */
    private String determineAttributeName(PIISpanAttribute annotation, String[] parameterNames, int parameterIndex) {
        // First priority: Use explicit annotation value if provided
        // This allows developers to specify meaningful, consistent attribute names
        if (!annotation.value().isEmpty()) {
            return annotation.value();
        }

        // Second priority: Use discovered parameter name if available
        // This provides reasonable default names that match the source code
        if (parameterNames != null && parameterIndex < parameterNames.length) {
            return parameterNames[parameterIndex];
        }

        // Final fallback: Generate a generic but unique attribute name
        // This ensures we always have a valid attribute name, even when parameter
        // names are not available due to compilation settings
        return "pii_arg" + parameterIndex;
    }
}
