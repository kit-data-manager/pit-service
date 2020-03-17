package edu.kit.datamanager.pit.typeregistry;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;

import edu.kit.datamanager.pit.EntityClass;

/**
 * Main abstraction interface towards the type registry. Contains all methods
 * required from the registry by the core services.
 * 
 */
public interface ITypeRegistry {

	/**
	 * Retrieves a property definition by its unique identifier.
	 * 
	 * @param propertyIdentifier
	 * @return the property definition or null if there is no such definition.
	 * @throws IOException
	 *             on communication errors with a remote registry
	 */
	public PropertyDefinition queryPropertyDefinition(String propertyIdentifier) throws IOException;

	/**
	 * Retrieves a property definition by its property name. Note that the name
	 * is not unique, thus the method returns a List.
	 * 
	 * @param propertyName
	 * @return a list with any number of entries (may be empty).
	 * @throws IOException
	 *             on communication errors with a remote registry
	 */
	public List<PropertyDefinition> queryPropertyDefinitionByName(String propertyName) throws IOException;

	/**
	 * Removes the property definition with given PID. If there is no definition
	 * with given PID, the method will do nothing.
	 * 
	 * @param propertyIdentifier
	 * @throws IOException
	 *             on communication errors with a remote registry
	 */
	public void removePropertyDefinition(String propertyIdentifier) throws IOException;

	/**
	 * Queries a type definition record from the type registry.
	 * 
	 * @param typeIdentifier
	 * @return a type definition record or null if the type is not registered.
	 * @throws IOException
	 *             on communication errors with a remote registry
	 */
	public TypeDefinition queryTypeDefinition(String typeIdentifier) throws IOException;

	public void createTypeDefinition(String typeIdentifier, TypeDefinition typeDefinition);

	/*
	 * Queries a profile definitino record from DTR
	 * 
	 * @param profileIdentifier
	 * 
	 * @return a profile definition record or null if the profile is not
	 * registered.
	 * 
	 * @throws IOException on communication errors with a remote registry
	 * 
	 * Added by Quan (Gabriel) Zhou @ Indiana University Bloomington
	 */
	public ProfileDefinition queryProfileDefinition(String profileIdentifier) throws IOException;

	public void createProfileDefinition(String profileIdentifier, ProfileDefinition profileDefinition);

	/**
	 * Generic query method. Requires no a priori knowledge about the kind of
	 * entity registered (property, type, ...).
	 * 
	 * @param identifier
	 * @return null if there is no object with given identifier, or an instance
	 *         of PropertyDefinition or TypeDefinition.
	 * @throws IOException
	 * @throws JsonProcessingException
	 */
	public Object query(String identifier) throws JsonProcessingException, IOException;

	/**
	 * Determines whether the given PID is an identifier in the type registry.
	 * Note that a positive answer does not necessarily mean the identifier is
	 * registered and resolvable.
	 * 
	 * @param pid
	 * @return true if the PID is generally acceptable for the type registry,
	 *         but may still be unregistered.
	 */
	public boolean isTypeRegistryPID(String pid);

	/**
	 * Determines whether the given identifier references a property or type.
	 * 
	 * @param identifier
	 * @return a value of {@link EntityClass}
	 * @throws IOException
	 *             on communication errors with a remote registry
	 */
	public EntityClass determineEntityClass(String identifier) throws IOException;

}
