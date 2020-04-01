package edu.kit.datamanager.pit.domain.old;

/**
 * Provides context for a property defined in type definition records, such as an optional/mandatory flag. 
 * 
 */
public class PropertyDefinitionParameters {
	
	private boolean mandatory;

	public PropertyDefinitionParameters(boolean mandatory) {
		super();
		this.mandatory = mandatory;
	}

	public boolean isMandatory() {
		return mandatory;
	}

}
