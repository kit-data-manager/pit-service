/*
 * Class author : Quan (Gabriel) Zhou @ Indiana University Bloomington
 */

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
public class ProfileDefinition {

	/**
	 * PID of the profile.
	 */
	@JsonProperty("identifier")
	protected String identifier;

	/**
	 * Value (boolean) True means mandatory, False means optional.
	 */
	@JsonProperty("types")
	protected HashMap<String, TypeDefinition> types;

	@JsonProperty("explanationOfUse")
	protected String explanationOfUse;

	@JsonProperty("description")
	protected String description;

	public ProfileDefinition(String identifier, String explanationOfUse, String description) {
		this.identifier = identifier;
		this.explanationOfUse = explanationOfUse;
		this.description = description;
		this.types = new HashMap<String, TypeDefinition>();
	}

	@JsonCreator
	public ProfileDefinition(@JsonProperty("identifier") String identifier,
			@JsonProperty("explanationOfUse") String explanationOfUse, @JsonProperty("description") String description,
			@JsonProperty("types") HashMap<String, TypeDefinition> types) {
		this.identifier = identifier;
		this.explanationOfUse = explanationOfUse;
		this.description = description;
		this.types = types;
	}

	public void addType(String typeIdentifier, TypeDefinition type_def) {
		this.types.put(typeIdentifier, type_def);
	}

	/**
	 * Returns a set of all properties. The caller will not be able to
	 * distinguish between mandatory and optional properties.
	 * 
	 * @return a set of property identifiers (strings)
	 */
	@JsonIgnore
	public Set<String> getAllTypes() {
		return new HashSet<String>(types.keySet());
	}

	public String getIdentifier() {
		return identifier;
	}
}
