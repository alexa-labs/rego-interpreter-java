// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.lib.expr;

import java.util.List;
import java.util.Map;

import javax.json.JsonObject;
import javax.json.JsonValue;

import com.amazon.antlr4.rego.interpreter.core.lib.JsonResolver;
import com.amazon.antlr4.rego.interpreter.type.Expression;
import com.amazon.antlr4.rego.interpreter.type.ExpressionArgument;

/**
 * Object functions from Rego language reference.
 * https://www.openpolicyagent.org/docs/latest/policy-reference/#objects-2
 */
public final class Objects {

    private Objects() {}

    private static final Expression GET = new
        @ExpressionArgument(JsonObject.class)
        @ExpressionArgument(JsonValue.class)
        @ExpressionArgument(JsonValue.class)
        Expression() {
            @Override
            public JsonValue run(List<JsonValue> args, JsonObject input, JsonObject data) {
                JsonObject obj = (JsonObject) args.get(0);
                Object key = JsonResolver.jsonToString(args.get(1));
                if (obj.containsKey(key)) {
                    return obj.get(key);
                }
                return args.get(2);
            }
        };

    public static final Map<String, Expression> EXPRESSIONS = Map.of(
        "object.get", GET
    );
}
