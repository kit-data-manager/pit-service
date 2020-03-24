package edu.kit.datamanager.pit.domain;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Encapsulates a type in the type registry, roughly defined as a set of
 * properties.
 * 
 */
public class TypeDefinition {

	/**
	 * PID of the type.
	 */
	@JsonProperty("identifier")
	protected String identifier;

	/**
	 * Value (boolean) True means mandatory, False means optional.
	 */
	@JsonProperty("properties")
	protected HashMap<String, PropertyDefinition> properties;

	@JsonProperty("explanationOfUse")
	protected String explanationOfUse;

	@JsonProperty("description")
	protected String description;

	public TypeDefinition(String identifier, String explanationOfUse, String description) {
		this.identifier = identifier;
		this.explanationOfUse = explanationOfUse;
		this.description = description;
		this.properties = new HashMap<String, PropertyDefinition>();
	}

	@JsonCreator
	public TypeDefinition(@JsonProperty("identifier") String identifier,
			@JsonProperty("explanationOfUse") String explanationOfUse, @JsonProperty("description") String description,
			@JsonProperty("properties") HashMap<String, PropertyDefinition> Properties) {
		this.identifier = identifier;
		this.explanationOfUse = explanationOfUse;
		this.description = description;
		this.properties = Properties;
	}

	public void addProperty(String propertyIdentifier, PropertyDefinition prop_def) {
		this.properties.put(propertyIdentifier, prop_def);
	}

	/**
	 * Returns a set of all properties. The caller will not be able to
	 * distinguish between mandatory and optional properties.
	 * 
	 * @return a set of property identifiers (strings)
	 */
	@JsonIgnore
	public Set<String> getAllProperties() {
		return new HashSet<String>(properties.keySet());
	}

	@JsonIgnore
	public Set<String> getProperties() {
		Set<String> result = new HashSet<>();
		for (String pd : properties.keySet()) {
			result.add(pd);
		}
		return result;
	}

	public String getIdentifier() {
		return identifier;
	}
}
