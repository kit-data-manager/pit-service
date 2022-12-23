package edu.kit.datamanager.pit.web.converter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import edu.kit.datamanager.pit.Application;
import edu.kit.datamanager.pit.domain.PIDRecord;
import edu.kit.datamanager.pit.domain.SimplePidRecord;

/**
 * Converts to-and-from PIDRecord format when SimplePidRecord format is actually
 * expected.
 * 
 * Do not use explicitly. Spring will use it, as explained below.
 * 
 * The idea is that all handlers in the REST API simply expect a serialized
 * (marshalled) version of a PID record. This is not always true, though. The
 * PIDRecord representation is pretty complex. To offer a simpler format,
 * `SimplePidRecord` was introduced. To avoid larger modifications within the
 * code, and to not break the API, the simple format comes into play only when
 * its content type is being used.
 * 
 * If the client wants to send in the simple format, it needs to set the
 * content-type header accordingly. Spring will then use this converter to
 * convert it directly into a PIDRecord instance, as the handler expects. This
 * way only one handler must be used for multiple formats.
 * 
 * For accepting formats, it is the same. With the accept header, a client may
 * control which format it would like to receive. If it prefers to receive the
 * simple format and sets the header accordingly, instead of directly
 * serializing the PIDRecord, this class will be used. It first converts the
 * record into the simple class representation before serializing into JSON.
 */
public class SimplePidRecordConverter implements HttpMessageConverter<PIDRecord> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimplePidRecordConverter.class);

    private boolean isValidMediaType(MediaType arg1) {
        return arg1.toString().contains(SimplePidRecord.CONTENT_TYPE_PURE);
    }

    @Override
    public boolean canRead(Class<?> arg0, MediaType arg1) {
        if (arg0 == null || arg1 == null) {
            return false;
        }
        LOGGER.trace("canRead: Checking applicability for class {} and mediatype {}.", arg0, arg1);
        return PIDRecord.class.equals(arg0) && isValidMediaType(arg1);
    }

    @Override
    public boolean canWrite(Class<?> arg0, MediaType arg1) {
        LOGGER.trace("canWrite: Checking applicability for class {} and mediatype {}.", arg0, arg1);
        return PIDRecord.class.equals(arg0) && isValidMediaType(arg1);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(
                MediaType.valueOf(SimplePidRecord.CONTENT_TYPE)
        );
    }

    @Override
    public PIDRecord read(Class<? extends PIDRecord> arg0, HttpInputMessage arg1)
            throws IOException, HttpMessageNotReadableException {
        LOGGER.trace("Read simple message from client and convert to PIDRecord.");
        try (InputStreamReader reader = new InputStreamReader(arg1.getBody(), StandardCharsets.UTF_8)) {
            String data = new BufferedReader(reader).lines().collect(Collectors.joining("\n"));
            return new PIDRecord(Application.jsonObjectMapper().readValue(data, SimplePidRecord.class));
        }
    }

    @Override
    public void write(PIDRecord arg0, MediaType arg1, HttpOutputMessage arg2)
            throws IOException, HttpMessageNotWritableException {
        LOGGER.trace("Write PIDRecord to simple format for client.");
        SimplePidRecord sim = new SimplePidRecord(arg0);
        byte[] simSerialized = Application.jsonObjectMapper().writeValueAsBytes(sim);
        arg2.getBody().write(simSerialized);
    }
}
