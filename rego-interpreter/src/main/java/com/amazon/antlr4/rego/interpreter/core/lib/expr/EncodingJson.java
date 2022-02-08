// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.lib.expr;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;

import com.amazon.antlr4.rego.interpreter.type.Expression;
import com.amazon.antlr4.rego.interpreter.type.ExpressionArgument;

/**
 * Json encoding functions from Rego language reference.
 * https://www.openpolicyagent.org/docs/latest/policy-reference/#encoding
 */
public final class EncodingJson {

    private EncodingJson() {}

    private static final Expression UNMARSHAL = new
        @ExpressionArgument(JsonString.class)
        Expression() {
            @Override
            public JsonValue run(List<JsonValue> args, JsonObject input, JsonObject data) {
                String jsonString = ((JsonString) args.get(0)).getString();
                InputStream is = new ByteArrayInputStream(jsonString.getBytes(StandardCharsets.UTF_8));
                JsonReader reader = Json.createReader(is);
                return reader.readObject();
            }
        };

    public static final Map<String, Expression> EXPRESSIONS = Map.of(
        "json.unmarshal", UNMARSHAL
    );
}
