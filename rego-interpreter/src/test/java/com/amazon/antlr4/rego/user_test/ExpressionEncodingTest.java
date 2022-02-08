// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.user_test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;

import com.amazon.antlr4.rego.interpreter.RegoExecutorBuilder;
import com.amazon.antlr4.rego.interpreter.core.lib.JsonResolver;
import com.amazon.antlr4.rego.interpreter.type.Expression;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ExpressionEncodingTest {

    // Accepts a list of bytes
    // Returns a byte[] encoded as JsonString
    private static final Expression BYTE_ARRAY_PRODUCER = (args, input, data) -> {
        JsonArray array = (JsonArray) args.get(0);
        byte[] bytes = new byte[array.size()];
        for (int i = 0; i < array.size(); i++) {
            bytes[i] = (byte) ((JsonNumber) array.get(i)).intValue();
        }
        String base64Bytes = new String(bytes, StandardCharsets.UTF_8);
        return JsonResolver.nativeToJson(base64Bytes);
    };

    // Accepts a byte[] encoded as JsonString, and an index
    // Returns value at index
    private static final Expression BYTE_ARRAY_CONSUMER = (args, input, data) -> {
        JsonString base64Bytes = (JsonString) args.get(0);
        JsonNumber index = (JsonNumber) args.get(1);
        byte[] bytes = base64Bytes.getString().getBytes(StandardCharsets.UTF_8);
        return JsonResolver.nativeToJson((long) bytes[index.intValue()]);
    };

    // Accepts a list of ints
    // Returns a int[] encoded as JsonString
    private static final Expression INT_ARRAY_PRODUCER = (args, input, data) -> {
        JsonArray array = (JsonArray) args.get(0);
        int[] ints = new int[array.size()];
        for (int i = 0; i < array.size(); i++) {
            ints[i] = ((JsonNumber) array.get(i)).intValue();
        }
        String base64Bytes = Arrays.toString(ints);
        return JsonResolver.nativeToJson(base64Bytes);
    };

    // Accepts a int[] encoded as JsonString, and an index
    // Returns value at index
    private static final Expression INT_ARRAY_CONSUMER = (args, input, data) -> {
        String encodedArray = ((JsonString) args.get(0)).getString();
        int[] array = Arrays.stream(encodedArray.substring(1, encodedArray.length()-1).split(","))
            .map(String::trim).mapToInt(Integer::parseInt).toArray();
        JsonNumber index = (JsonNumber) args.get(1);
        return JsonResolver.nativeToJson((long) array[index.intValue()]);
    };

    // Accepts a int[] encoded as JsonString
    // Returns a JsonArray of JsonNumbers
    private static final Expression INT_ARRAY_DECODER = (args, input, data) -> {
        String encodedArray = ((JsonString) args.get(0)).getString();
        Stream<?> array = Arrays.stream(encodedArray.substring(1, encodedArray.length()-1).split(","))
            .map(String::trim).mapToLong(Long::parseLong).boxed();
        return JsonResolver.nativeToJson(array);
    };

    private static final Map<String, Expression> RUNTIME_EXPR_MAP = Map.of(
        "Encoded.Byte.Array.Producer", BYTE_ARRAY_PRODUCER,
        "Encoded.Byte.Array.Consumer", BYTE_ARRAY_CONSUMER,
        "Encoded.Int.Array.Producer", INT_ARRAY_PRODUCER,
        "Encoded.Int.Array.Consumer", INT_ARRAY_CONSUMER,
        "Encoded.Int.Array.Decoder", INT_ARRAY_DECODER
    );

    private static final String POLICY1 = "package ArrayPolicy;"

                                        + "# Demonstrate byte[] passed as string\n"
                                        + "ba := Encoded.Byte.Array.Producer([75, 33, 82]);"
                                        + "ba1 := Encoded.Byte.Array.Consumer(ba, 1);"
                                        + "bap { ba1 == 33 };"

                                        + "# Demonstrate int[] passed as string\n"
                                        + "ia := Encoded.Int.Array.Producer([1000, 4328, 99653]);"
                                        + "ia1 := Encoded.Int.Array.Consumer(ia, 2);"
                                        + "iap { ia1 == 99653 };"

                                        + "# Demonstrate int[] passed as JsonArray of JsonNumber\n"
                                        + "da := Encoded.Int.Array.Decoder(ia);"
                                        + "dap { da[1] == 4328 };";

    @Test
    void expressionEncodingTests() throws Exception {
        JsonObject output = new RegoExecutorBuilder(POLICY1).expressions(RUNTIME_EXPR_MAP).build().executePolicy("{}");
        Assertions.assertTrue(output.getBoolean("bap"));
        Assertions.assertTrue(output.getBoolean("iap"));
        Assertions.assertTrue(output.getBoolean("dap"));
    }
}
