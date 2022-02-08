// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.lib.expr;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonValue;

import com.amazon.antlr4.rego.interpreter.core.lib.JsonResolver;
import com.amazon.antlr4.rego.interpreter.type.Expression;
import com.amazon.antlr4.rego.interpreter.type.ExpressionArgument;

/**
 * Array functions from Rego language reference.
 * https://www.openpolicyagent.org/docs/latest/policy-reference/#arrays-2
 */
public final class Array {

    private Array() {}

    private static final Expression CONCAT = new
        @ExpressionArgument(JsonArray.class)
        @ExpressionArgument(JsonArray.class)
        Expression() {
            @Override
            public JsonValue run(List<JsonValue> args, JsonObject input, JsonObject data) {
                JsonArray a0 = (JsonArray) args.get(0);
                JsonArray a1 = (JsonArray) args.get(1);
                return JsonResolver.mergeArrays(a0, a1);
            }
        };

    private static final Expression SLICE = new
        @ExpressionArgument(JsonArray.class)
        @ExpressionArgument(JsonNumber.class)
        @ExpressionArgument(JsonNumber.class)
        Expression() {
            @Override
            public JsonValue run(List<JsonValue> args, JsonObject input, JsonObject data) {
                JsonArray array = (JsonArray) args.get(0);
                int start = ((JsonNumber) args.get(1)).intValue();
                int end = ((JsonNumber) args.get(2)).intValue();
                if (start < 0) {
                    start = 0;
                }
                if (end > array.size()) {
                    end = array.size();
                }
                if (start >= end) {
                    return JsonResolver.nativeToJson(Stream.empty());
                }
                return JsonResolver.sliceArray(array, start, end);
            }
        };

    public static final Map<String, Expression> EXPRESSIONS = Map.of(
        "array.concat", CONCAT,
        "array.slice", SLICE
    );
}
