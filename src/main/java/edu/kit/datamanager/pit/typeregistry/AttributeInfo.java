package edu.kit.datamanager.pit.typeregistry;

import org.everit.json.schema.Schema;

import java.util.List;

/**
 * @param pid the pid of this attribute
 * @param name a human-readable name, defined in the DTR
 * @param typeName name of the schema type of this attribute in the DTR,
 *                e.g. "Profile", "InfoType", "Special-Info-Type", ...
 * @param jsonSchema the json schema to validate a value of this attribute
 */
public record AttributeInfo(
        String pid,
        String name,
        String typeName,
        Schema jsonSchema
) {}
