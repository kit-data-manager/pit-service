/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kit.datamanager.pit.validation.impl;

import edu.kit.datamanager.pit.validation.ITypeValidator;
import java.util.regex.Pattern;

/**
 *
 * @author Torridity
 */
public class HandleValidator implements ITypeValidator {

    private final static String PATTERN = "^([0-9,A-Z,a-z])+" // 21
            + "([.]){1,1}([0-9,A-Z,a-z])+" // .T11148
            + "([/]){1,1}([^-~])+$"; // /3626040cadcac1571685

    @Override
    public boolean applicableToIdentifier(String identifier) {
        return "21.T11148/3626040cadcac1571685".equalsIgnoreCase(identifier);
    }

    @Override
    public boolean isValid(String value) {
        return Pattern.compile(PATTERN).matcher(value).matches();
    }

    public static void main(String[] args) {
        System.out.println(new HandleValidator().isValid("21.T11148/3626040cadcac1571685"));
    }

}
