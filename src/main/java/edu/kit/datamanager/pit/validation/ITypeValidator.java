/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.pit.validation;

/**
 *
 * @author Torridity
 */
public interface ITypeValidator {

    boolean applicableToIdentifier(String identifier);

    boolean isValid(String value);
}
