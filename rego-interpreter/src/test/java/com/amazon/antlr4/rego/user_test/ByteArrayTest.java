// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.user_test;

import java.util.Arrays;
import java.util.Base64;
import java.util.Random;

import javax.json.JsonObject;
import javax.json.JsonValue;

import com.amazon.antlr4.rego.interpreter.RegoExecutorBuilder;
import com.amazon.antlr4.rego.interpreter.type.json.JsonByteArray;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ByteArrayTest {

    private static final String POLICY_TEXT = "package byteArrayTest "
                                            + "decoded := base64.decode(input.bytes)";

    @Test
    void testByteArrayDecode() throws Exception {
        byte[] rawBytes = new byte[20];
        new Random().nextBytes(rawBytes);
        String encodedBytes = Base64.getEncoder().encodeToString(rawBytes);
        String jsonInput = "{ \"bytes\": \"" + encodedBytes + "\"}";
        JsonObject output = new RegoExecutorBuilder(POLICY_TEXT).build().executePolicy(jsonInput);

        JsonValue decodedValue = output.get("decoded");
        Assertions.assertTrue(decodedValue instanceof JsonByteArray);
        byte[] outputBytes = ((JsonByteArray) decodedValue).getBytes();
        Assertions.assertTrue(Arrays.equals(rawBytes, outputBytes));
    }
}
