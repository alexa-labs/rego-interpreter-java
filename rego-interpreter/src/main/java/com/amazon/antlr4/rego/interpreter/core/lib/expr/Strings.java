// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.lib.expr;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import com.amazon.antlr4.rego.interpreter.core.lib.JsonResolver;
import com.amazon.antlr4.rego.interpreter.core.type.RegoProcessorException;
import com.amazon.antlr4.rego.interpreter.type.Expression;
import com.amazon.antlr4.rego.interpreter.type.ExpressionArgument;

/**
 * String functions from Rego language reference.
 * https://www.openpolicyagent.org/docs/latest/policy-reference/#strings
 */
public final class Strings {

    private Strings() {}

    private static final Expression CONCAT = new
        @ExpressionArgument(JsonString.class)
        @ExpressionArgument(JsonArray.class)
        Expression() {
            @Override
            public JsonValue run(List<JsonValue> args, JsonObject input, JsonObject data) {
                String delimiter = ((JsonString) args.get(0)).getString();
                JsonArray values = (JsonArray) (args.get(1));
                return JsonResolver.nativeToJson(JsonResolver.concat(delimiter, values));
            }
        };

    private static final Expression CONTAINS = new
        @ExpressionArgument(JsonString.class)
        @ExpressionArgument(JsonString.class)
        Expression() {
            @Override
            public JsonValue run(List<JsonValue> args, JsonObject input, JsonObject data) {
                String string = ((JsonString) args.get(0)).getString();
                String search = ((JsonString) args.get(1)).getString();
                return JsonResolver.nativeToJson(string.contains(search));
            }
        };

    private static final Expression ENDSWITH = new
        @ExpressionArgument(JsonString.class)
        @ExpressionArgument(JsonString.class)
        Expression() {
            @Override
            public JsonValue run(List<JsonValue> args, JsonObject input, JsonObject data) {
                String string = ((JsonString) args.get(0)).getString();
                String search = ((JsonString) args.get(1)).getString();
                return JsonResolver.nativeToJson(string.endsWith(search));
            }
        };

    private static final Expression STARTSWITH = new
        @ExpressionArgument(JsonString.class)
        @ExpressionArgument(JsonString.class)
        Expression() {
            @Override
            public JsonValue run(List<JsonValue> args, JsonObject input, JsonObject data) {
                String string = ((JsonString) args.get(0)).getString();
                String search = ((JsonString) args.get(1)).getString();
                return JsonResolver.nativeToJson(string.startsWith(search));
            }
        };

    private static final Expression INDEXOF = new
        @ExpressionArgument(JsonString.class)
        @ExpressionArgument(JsonString.class)
        Expression() {
            @Override
            public JsonValue run(List<JsonValue> args, JsonObject input, JsonObject data) {
                String string = ((JsonString) args.get(0)).getString();
                String search = ((JsonString) args.get(1)).getString();
                return JsonResolver.nativeToJson(string.indexOf(search));
            }
        };

    private static final Expression SUBSTRING = new
        @ExpressionArgument(JsonString.class)
        @ExpressionArgument(JsonNumber.class)
        @ExpressionArgument(JsonNumber.class)
        Expression() {
            @Override
            public JsonValue run(List<JsonValue> args, JsonObject input, JsonObject data) {
                String string = ((JsonString) args.get(0)).getString();
                int startIndex = ((JsonNumber) args.get(1)).intValue();
                int endIndex;

                if (startIndex < 0) {
                    throw new RegoProcessorException.RuleEvaluationException("startIndex must be an integer >=0");
                }
                if (startIndex > string.length()) {
                    return JsonResolver.nativeToJson("");
                }

                int lengthToExtract = ((JsonNumber) args.get(2)).intValue();

                if (lengthToExtract < 0) {
                    endIndex = string.length();
                } else {
                    endIndex = Math.min(startIndex + lengthToExtract, string.length());
                }
                return JsonResolver.nativeToJson(string.substring(startIndex, endIndex));
            }
        };

    private static final Expression LOWER = new
        @ExpressionArgument(JsonString.class)
        Expression() {
            @Override
            public JsonValue run(List<JsonValue> args, JsonObject input, JsonObject data) {
                String string = ((JsonString) args.get(0)).getString();
                return JsonResolver.nativeToJson(string.toLowerCase());
            }
        };

    private static final Expression UPPER = new
        @ExpressionArgument(JsonString.class)
        Expression() {
            @Override
            public JsonValue run(List<JsonValue> args, JsonObject input, JsonObject data) {
                String string = ((JsonString) args.get(0)).getString();
                return JsonResolver.nativeToJson(string.toUpperCase());
            }
        };

    private static final Expression FORMAT_INT = new
        @ExpressionArgument(JsonNumber.class)
        @ExpressionArgument(JsonNumber.class)
        Expression() {
            @Override
            public JsonValue run(List<JsonValue> args, JsonObject input, JsonObject data) {
                BigInteger number = ((JsonNumber) args.get(0)).bigIntegerValue();
                int base = ((JsonNumber) args.get(1)).intValue();
                if (base != 2 && base != 8 && base != 10 && base != 16) {
                    throw new RegoProcessorException.RuleEvaluationException(
                        String.format("Cannot format number to base %d", base)
                    );
                }
                return JsonResolver.nativeToJson(number.toString(base));
            }
        };

    public static final Map<String, Expression> EXPRESSIONS = Map.of(
        "concat", CONCAT,
        "contains", CONTAINS,
        "endswith", ENDSWITH,
        "indexof", INDEXOF,
        "lower", LOWER,
        "startswith", STARTSWITH,
        "upper", UPPER,
        "format_int", FORMAT_INT,
        "substring", SUBSTRING
    );
}
