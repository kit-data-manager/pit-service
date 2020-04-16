/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.pit.validation.impl;

import edu.kit.datamanager.pit.validation.ITypeValidator;
import java.util.Arrays;

/**
 *
 * @author Torridity
 */
public class LicenseValidator implements ITypeValidator {

    private final String[] LICENSES = {"CC0", "CC-BY", "CC-BY-SA", "CC-by-NC", "CC-BY-ND", "CC-BY-NC-SA", "CC-BY-NC-ND"};

    @Override
    public boolean applicableToIdentifier(String identifier) {
        return "21.T11148/dc54ae4b6807f5887fda".equalsIgnoreCase(identifier);
    }

    @Override
    public boolean isValid(String value) {
        for (String license : LICENSES) {
            if (license.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        System.out.println(new LicenseValidator().isValid("CC-BY"));
    }

}
