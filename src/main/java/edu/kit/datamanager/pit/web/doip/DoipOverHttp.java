/**
 * A lot of code re-used from metadataHub, Apache 2.0 License.
 * 
 * Original Code: https://git.rwth-aachen.de/nfdi4ing/s-3/s-3-3/metadatahub/-/blob/main/src/main/java/edu/kit/metadatahub/rest/controller/Rest4DoipController.java
 */

package edu.kit.datamanager.pit.web.doip;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.kit.datamanager.pit.pitservice.ITypingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * REST controller for DOIP over HTTP.
 */
@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "The request was processed successfully."),
        @ApiResponse(responseCode = "400", description = "There was something wrong with the structure or content of the request"),
        @ApiResponse(responseCode = "401", description = "The client must authenticate to perform the attempted operation"),
        @ApiResponse(responseCode = "403", description = "The client was not permitted to perform the attempted operation"),
        @ApiResponse(responseCode = "404", description = "The requested digital object could not be found"),
        @ApiResponse(responseCode = "409", description = "There was a conflict preventing the request from being executed"),
        @ApiResponse(responseCode = "500", description = "There was an internal server error") })
public class DoipOverHttp {

    private static final Logger LOGGER = LoggerFactory.getLogger(DoipOverHttp.class);

    private final String SELF_IDENTIFIER_FALLBACK = "self";
    private final Collection<DoipOperationId> SERVICE_OPERATIONS = List.of(
        //DoipOperationId.OP_HELLO,
        //DoipOperationId.OP_CREATE,
        //DoipOperationId.OP_RETRIEVE,
        //DoipOperationId.OP_SEARCH,
        DoipOperationId.OP_LIST
    );
    private final Collection<DoipOperationId> FAIRDO_OPERATIONS = List.of(
        //DoipOperationId.OP_UPDATE,
        //DoipOperationId.OP_DELETE,
        DoipOperationId.OP_LIST
        //DoipOperationId.OP_VALIDATE
    );

    @Autowired
    ITypingService pit;

    public DoipResponse op_server_create(
            final DoipOperationId operationId,
            final String targetId,
            final String clientId,
            final JsonNode attributes,
            final DoipRequest body
    ) {
        throw new NotImplementedException();
    }

    /**
     * DOIP Entpoint. Basically executes targetId.operationId(attributes, body, clientId).
     */
    @Operation(summary = "DOIPv2 over HTTP.", description = "DOIPv2 via HTTP as defined by Cordra, as far as applicable.")
    @PostMapping(value = { "/doip" }, consumes = { MediaType.ALL_VALUE })
    @ResponseBody
    public ResponseEntity<DoipResponse> postDoipOperation(
            @Parameter(description = "The operationId.", required = true)
            @RequestParam(value = "operationId")
            final DoipOperationId operationId,

            @Parameter(description = "The targetId.", required = true)
            @RequestParam(value = "targetId")
            final String targetId,

            @Parameter(description = "the identifier of the caller", required = true)
            @RequestParam(value = "clientId")
            final String clientId,

            @Parameter(description = "arbitrary JSON to inform the operation", required = true)
            @RequestParam(value = "attributes")
            final JsonNode attributes,

            @Parameter(description = "Json representation of the Digital Object.", required = false)
            @RequestBody(required = false)
            final DoipRequest body
    ) {
        // Authentication: is done by security layer (keycloak / JWT)

        boolean isServiceTarget = Objects.equals(targetId, SELF_IDENTIFIER_FALLBACK) /* TODO or a real identifier configured for this service? */;

        if (isServiceTarget) {
            switch (operationId) {
                case OP_LIST:
                    return operation_service_list(targetId, operationId, body, attributes, clientId);
                //case OP_HELLO:
                //    break;
                //case OP_CREATE:
                //    break;
                //case OP_RETRIEVE:
                //    break;
                //case OP_SEARCH:
                //    break;
                default:
                    DoipResponse r = new DoipResponse(DoipStatus.REQUEST_INVALID);
                    return new ResponseEntity<>(r, r.getDoipStatus().getHttpStatus());
            }
        } else {
            // check if identifier is registered
            if (pit.isIdentifierRegistered(targetId)) {
                switch (operationId) {
                    case OP_LIST:
                        return operation_fairdo_list(targetId, operationId, body, attributes, clientId);
                    //case OP_UPDATE:
                    //    break;
                    //case OP_DELETE:
                    //    break;
                    //case OP_VALIDATE:
                    //    break;
                    default:
                        DoipResponse r = new DoipResponse(DoipStatus.REQUEST_INVALID);
                        return new ResponseEntity<>(r, r.getDoipStatus().getHttpStatus());
                }
            } else {
                DoipResponse r = new DoipResponse(DoipStatus.DIGITAL_OBJECT_NOT_FOUND);
                return new ResponseEntity<>(r, r.getDoipStatus().getHttpStatus());
            }
            // if not, return error
        }
    }

    private ResponseEntity<DoipResponse> operation_fairdo_list(
        String targetId,
        DoipOperationId operationId,
        DoipRequest body,
        JsonNode attributes,
        String clientId
    ){
        ObjectMapper mapper = new ObjectMapper();
        DoipResponse r = new DoipResponse(DoipStatus.OPERATION_SUCCESS);
        // TODO add other known operations
        r.setOutput(mapper.valueToTree(FAIRDO_OPERATIONS));
        return new ResponseEntity<>(r, r.getDoipStatus().getHttpStatus());
    }

    private ResponseEntity<DoipResponse> operation_service_list(
        String targetId,
        DoipOperationId operationId,
        DoipRequest body,
        JsonNode attributes,
        String clientId
    ) {
        ObjectMapper mapper = new ObjectMapper();
        DoipResponse r = new DoipResponse(DoipStatus.OPERATION_SUCCESS);
        r.setOutput(mapper.valueToTree(SERVICE_OPERATIONS));
        return new ResponseEntity<>(r, r.getDoipStatus().getHttpStatus());
	}

}
