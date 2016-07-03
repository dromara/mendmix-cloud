package com.jeesuite.common.packagescan;

import java.util.regex.Pattern;

/**
 * A simple wildcard pattern that uses the '*' character to match everything.
 */
public class SimpleWildcardPattern implements CompiledPattern {
    private Pattern pattern;
    private String original;

    public SimpleWildcardPattern(String pattern) {

        this.original = pattern;

        String ptn = pattern;
        ptn = ptn.replace(".", "\\.");
        ptn = ptn.replace("*", ".*");
        this.pattern = Pattern.compile(ptn);
    }


    public String getOriginal() {
        return original;
    }

    public boolean matches(String value) {
        return pattern.matcher(value).matches();
    }
}
