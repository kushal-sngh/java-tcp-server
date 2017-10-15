package com.kush.newrelic.util;

import org.springframework.stereotype.Component;

/**
 * Created by root on 7/28/17.
 */
@Component
public class ValidateInput {
    public static final String NEW_LINE = System.lineSeparator();

    public boolean validate(String str) {

        if (str == null || str.isEmpty())
            return false;

        if (str.matches("^[0-9]{9}[\n]$") || str.matches("^[0-9]{9}[\r?\n]$"))
            return true;

        return false;
    }
}