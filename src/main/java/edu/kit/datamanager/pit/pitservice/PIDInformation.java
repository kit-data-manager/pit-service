package edu.kit.datamanager.pit.pitservice;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import edu.kit.datamanager.pit.typeregistry.TypeDefinition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Class for storing and managing a PID record that carries meta-information
 * such as conformance flags.
 * 
 */
@JsonInclude(Include.NON_EMPTY)
public class PIDInformation {

	private static class PIDInformationPropertyEntry {

		/**
		 * The name of the property, which may be empty, however this does not
		 * indicate that the property does not have a name (it simply was not
		 * retrieved).
		 */
		@JsonProperty("name")
		private String name;

		/**
		 * The value of the property.
		 */
		@JsonProperty("value")
		private String value;

		@JsonCreator
		public PIDInformationPropertyEntry(@JsonProperty("name") String name, @JsonProperty("value") String value) {
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public String getValue() {
			return value;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

	@JsonProperty("values")
	private HashMap<String, PIDInformationPropertyEntry> propertyValues;

	@JsonProperty("conformance")
	private HashMap<String, Boolean> conformance = new HashMap<>();

	public PIDInformation() {
		super();
		this.propertyValues = new HashMap<>();
	}

	@JsonCreator
	public PIDInformation(@JsonProperty("values") HashMap<String, PIDInformationPropertyEntry> propertyValues,
			@JsonProperty("conformance") HashMap<String, Boolean> conformance) {
		super();
		this.propertyValues = propertyValues;
		this.conformance = conformance;
	}

	/**
	 * Checks whether the stored property values conform to the given type and
	 * stores the result of the conformance checks in the local information
	 * record.
	 * 
	 * @param typeDef
	 * @return true if all mandatory properties of the type are present
	 */
	public boolean checkTypeConformance(TypeDefinition typeDef) {
		boolean conf = true;
		for (String p : typeDef.getProperties()) {
			if (!propertyValues.containsKey(p)) {
				conf = false;
				break;
			}
		}
		conformance.put(typeDef.getIdentifier(), conf);
		return conf;
	}

	/**
	 * Returns the value of a single property given the property identifier. The
	 * property name is ignored.
	 * 
	 * @param propertyIdentifier
	 * @return the value or an empty String if there is no such property
	 */
	@JsonIgnore
	public String getPropertyValue(String propertyIdentifier) {
		PIDInformationPropertyEntry pe = propertyValues.get(propertyIdentifier);
		if (pe == null)
			return "";
		return pe.getValue();
	}

	public void addProperty(String propertyIdentifier, String propertyName, String propertyValue) {
		if (propertyIdentifier.isEmpty())
			throw new IllegalArgumentException("The identifier of a property may not be empty!");
		propertyValues.put(propertyIdentifier, new PIDInformationPropertyEntry(propertyName, propertyValue));
	}

	@JsonIgnore
	public Set<String> getPropertyIdentifiers() {
		return propertyValues.keySet();
	}

	/**
	 * Sets the name of an already listed property, given its identifier.
	 * 
	 * @param propertyIdentifier
	 * @param name
	 */
	@JsonIgnore
	public void setPropertyName(String propertyIdentifier, String name) {
		PIDInformationPropertyEntry pe = propertyValues.get(propertyIdentifier);
		if (pe == null)
			throw new IllegalArgumentException("Property identifier not listed in this record: " + propertyIdentifier);
		pe.setName(name);
	}

	public boolean hasProperty(String propertyIdentifier) {
		return propertyValues.containsKey(propertyIdentifier);
	}

	/**
	 * Removes all properties that are not listed in the given collection.
	 * 
	 * @param propertiesToKeep
	 *            a collection of property identifiers to keep.
	 */
	public void removePropertiesNotListed(Collection<String> propertiesToKeep) {
		Iterator<String> iter = propertyValues.keySet().iterator();
		while (iter.hasNext()) {
			String propID = iter.next();
			if (!propertiesToKeep.contains(propID))
				iter.remove();
		}
	}
}
