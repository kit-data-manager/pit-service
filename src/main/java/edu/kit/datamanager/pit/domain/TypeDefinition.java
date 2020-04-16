/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.pit.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import lombok.Data;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;

/**
 *
 * @author Torridity
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TypeDefinition {

    private String name;
    private String identifier;
    private String description;
    private boolean optional = false;
    private boolean repeatable = false;
    private String expression;
    private String value;
    private Schema jsonSchema;

    private ProvenanceInformation provenance;
    @JsonProperty("properties")
    private Map<String, TypeDefinition> subTypes = new HashMap<>();

    @JsonIgnore
    private TypeDefinition resolvedTypeDefinition;

    @JsonIgnore
    public Set<String> getAllProperties() {
        Set<String> props = new HashSet<>();
        Set<Entry<String, TypeDefinition>> entries = subTypes.entrySet();
        entries.forEach((entry) -> {
            props.add(entry.getKey());
        });

        return props;
    }

    public void setSchema(String schema) {
        if (schema == null) {
            return;
        }

        JSONObject jsonSchema = new JSONObject(schema);
        this.jsonSchema = SchemaLoader.load(jsonSchema);
        // schema.validate(jsonSubject);
    }
//^([0-9]{4})-([0]?[1-9]|1[0-2])-([0-2][0-9]|3[0-1])(T([0-1][0-9]|2[0-3]):([0-5][0-9]):([0-5][0-9](.[0-9]*)?(Z|([+|-]([0-1][0-9]|2[0-3]):[0-5][0-9])){1}))$
//^([0-9]{4})([-]){1,1}[0]?[1-9]|1[0-2])([-]){1,1}([0-2][0-9]|3[0-1])(T([0-1][0-9]|2[0-3])([:]){1,1}([0-5][0-9])([:]){1,1}([0-5][0-9](.[0-9]*)?(Z|([+|-]([0-1][0-9]|2[0-3]):[0-5][0-9])){1}))$"

    public boolean validate(String document) {
        System.out.println("VALIDATE " + document);
        if (jsonSchema != null) {
            Object toValidate = document;
            if (document.startsWith("{")) {
                toValidate = new JSONObject(document);
            }
            try {
                jsonSchema.validate(toValidate);
                System.out.println("Is Valid!");
            } catch (ValidationException ex) {
                ex.printStackTrace();
                System.out.println("FAILED!");
                return false;
            }
        }
        return true;
    }

    public boolean isOptional(String property) {
        return subTypes.get(property).isOptional();
    }

    public void addSubType(TypeDefinition subType) {
        subTypes.put(subType.getIdentifier(), subType);
    }

    public static void main(String[] args) throws Exception {

        String type = "{\n"
                + "   \"identifier\": \"21.T11148/1c699a5d1b4ad3ba4956\",\n"
                + "   \"name\": \"digitalObjectType\",\n"
                + "   \"description\": \"Handle points to type definition in DTR for this type of object. Distinguishing metadata from data objects is a client decision within a particular usage context, which may to some extent rely on the digitalObjectType value provided. (context : KernelInformation)\\n\",\n"
                + "   \"standards\": [{\n"
                + "         \"natureOfApplicability\": \"depends\",\n"
                + "         \"name\": \"21.T11148/3626040cadcac1571685\",\n"
                + "         \"issuer\": \"DTR\"\n"
                + "      }],\n"
                + "   \"provenance\": {\n"
                + "      \"contributors\": [{\n"
                + "            \"identifiedUsing\": \"Text\",\n"
                + "            \"name\": \"Ulrich Schwardmann\",\n"
                + "            \"details\": \"GWDG\"\n"
                + "         }],\n"
                + "      \"creationDate\": \"2019-04-01T11:01:52.469Z\",\n"
                + "      \"lastModificationDate\": \"2019-11-14T12:28:19.011Z\"\n"
                + "   },\n"
                + "   \"representationsAndSemantics\": [{\n"
                + "         \"expression\": \"\",\n"
                + "         \"value\": \"\",\n"
                + "         \"subSchemaRelation\": \"denyAdditionalProperties\",\n"
                + "         \"allowAbbreviatedForm\": \"Yes\"\n"
                + "      }],\n"
                + "   \"properties\": [{\n"
                + "         \"name\": \"digitalObjectType\",\n"
                + "         \"identifier\": \"21.T11148/3626040cadcac1571685\",\n"
                + "         \"representationsAndSemantics\": [{\n"
                + "               \"expression\": \"\",\n"
                + "               \"value\": \"\",\n"
                + "               \"obligation\": \"Mandatory\",\n"
                + "               \"repeatable\": \"No\",\n"
                + "               \"allowOmitSubsidiaries\": \"Yes\"\n"
                + "            }]\n"
                + "      }],\n"
                + "   \"validationSchema\": \"{\\\"definitions\\\": {\\\"21.T11148_3626040cadcac1571685\\\": {\\\"pattern\\\": \\\"^([0-9,A-Z,a-z])+(\\\\\\\\.([0-9,A-Z,a-z])+)*\\\\\\\\/([!-~])+$\\\", \\\"type\\\": \\\"string\\\", \\\"description\\\": \\\"Handle-Identifier-ASCII@21.T11148/3626040cadcac1571685\\\"}}, \\\"$schema\\\": \\\"http://json-schema.org/draft-04/schema#\\\", \\\"description\\\": \\\"digitalObjectType@21.T11148/1c699a5d1b4ad3ba4956\\\", \\\"$ref\\\": \\\"#/definitions/21.T11148_3626040cadcac1571685\\\"}\"\n"
                + "}";

        ObjectMapper mapper = new ObjectMapper();//.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        TypeDefinition def = mapper.readValue(type, TypeDefinition.class);
        System.out.println("DEF " + def);

    }

}
