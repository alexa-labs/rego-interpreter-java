// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.lib.expr;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import com.amazon.antlr4.rego.interpreter.core.lib.JsonResolver;
import com.amazon.antlr4.rego.interpreter.type.Expression;
import com.amazon.antlr4.rego.interpreter.type.ExpressionArgument;

/**
 * Array functions from Rego language reference.
 * https://www.openpolicyagent.org/docs/latest/policy-reference/#sets-2
 */
public final class Set {

    private Set() {}

    private static final Expression INTERSECTION = new
        @ExpressionArgument(JsonArray.class)
        Expression() {
            @Override
            public JsonValue run(List<JsonValue> args, JsonObject input, JsonObject data) {
                JsonArray a0 = (JsonArray) args.get(0);
                java.util.Set<JsonValue> mergedSet = new HashSet<>(a0.getJsonArray(0));
                for (int i = 0; i < a0.size(); i++) {
                    mergedSet.retainAll(a0.getJsonArray(i));
                }
                return JsonResolver.nativeToJson(mergedSet);
            }
        };

    private static final Expression UNION = new
    @ExpressionArgument(JsonArray.class)
    Expression() {
        @Override
        public JsonValue run(List<JsonValue> args, JsonObject input, JsonObject data) {
            JsonArray a0 = (JsonArray) args.get(0);
            java.util.Set<JsonValue> mergedSet = new HashSet<>(a0.getJsonArray(0));
            for (int i = 0; i < a0.size(); i++) {
                mergedSet.addAll(a0.getJsonArray(i));
            }
            return JsonResolver.nativeToJson(mergedSet);
        }
    };

    private static final Expression EMPTY_SET = new
    Expression() {
        @Override
        public JsonValue run(List<JsonValue> args, JsonObject input, JsonObject data) {
            return JsonResolver.nativeToJson(Collections.EMPTY_SET);
        }
    };

    public static final Map<String, Expression> EXPRESSIONS = Map.of(
        "intersection", INTERSECTION,
        "union", UNION,
        "set", EMPTY_SET
    );
}
