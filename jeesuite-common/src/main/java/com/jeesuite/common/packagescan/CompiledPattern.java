package com.jeesuite.common.packagescan;

/**
 * A wildcard pattern that has been compiled
 */
public interface CompiledPattern {

    /**
     * @return The original pattern
     */
    String getOriginal();

    /**
     * Tries to match a value
     * @param value The value to match
     * @return True if fully matched
     */
    boolean matches(String value);
}
