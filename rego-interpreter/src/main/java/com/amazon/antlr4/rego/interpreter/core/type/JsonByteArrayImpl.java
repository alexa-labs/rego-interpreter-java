// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.type;

import java.nio.charset.StandardCharsets;

import com.amazon.antlr4.rego.interpreter.type.json.JsonByteArray;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Allows byte[] to be passed around in Rego as a native type.
 * <p>
 * Json has no native support for byte[] so it has to be converted to String.
 * However, String cannot reliably convert between UTF-8 and byte[].
 * <p>
 * This class masquerades as JsonString for normal use.
 */
public final class JsonByteArrayImpl implements JsonByteArray {

    private final byte[] value;
    private final String stringValue;

    /**
     * Stores value by reference for performance.
     * Make copy before construction to protect original.
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public JsonByteArrayImpl(byte[] value) {
        this.value = value;
        this.stringValue = new String(value, StandardCharsets.UTF_8);
    }

    @Override
    public ValueType getValueType() {
        return ValueType.STRING;
    }

    @Override
    public String getString() {
        return stringValue;
    }

    @Override
    public CharSequence getChars() {
        return stringValue;
    }

    /**
     * Returns value by reference for performance.
     * Make copy before using to protect returned value.
     */
    @Override
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public byte[] getBytes() {
        return value;
    }
}
