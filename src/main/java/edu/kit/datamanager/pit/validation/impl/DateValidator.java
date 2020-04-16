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
public class DateValidator implements ITypeValidator {

    private final static String PATTERN = "^([0-9]{4})([-]){1,1}"
            + "([0]?[1-9]|1[0-2])([-]){1,1}"
            + "([0-2][0-9]|3[0-1])(T([0-1][0-9]|2[0-3])([:]){1,1}"
            + "([0-5][0-9])([:])"
            + "{1,1}([0-5][0-9](.[0-9]*)?(Z|([+|-]([0-1][0-9]|2[0-3]):[0-5][0-9])){1}))$";

    @Override
    public boolean applicableToIdentifier(String identifier) {
        return "21.T11148/3bfb2839a6967114bc3e".equalsIgnoreCase(identifier);
    }

    @Override
    public boolean isValid(String value) {
        return Pattern.compile(PATTERN).matcher(value).matches();
    }

    public static void main(String[] args) {
        System.out.println(new DateValidator().isValid("2017-02-09T13:13:07.947Z"));

    }

}
