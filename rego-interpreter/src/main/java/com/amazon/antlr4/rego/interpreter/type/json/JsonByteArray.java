// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.type.json;

import javax.json.JsonString;

/**
 * Allows byte[] to be passed around in Rego as a native type.
 * <p>
 * WARNING: JsonByteArray passes the array by reference for performance.
 * This means that (unlike other Json types) the array is mutable.
 * <p>
 * Json has no native support for byte[] so it has to be converted to String.
 * However, String cannot reliably convert between UTF-8 and byte[].
 * <p>
 * This class masquerades as a JsonString for normal use.
 */
public interface JsonByteArray extends JsonString {

    byte[] getBytes();
}
