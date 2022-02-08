// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.lib.expr;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import com.amazon.antlr4.rego.interpreter.core.lib.JsonResolver;
import com.amazon.antlr4.rego.interpreter.core.type.RegoProcessorException;
import com.amazon.antlr4.rego.interpreter.type.Expression;
import com.amazon.antlr4.rego.interpreter.type.ExpressionArgument;

/**
 * Base64 encoding functions from Rego language reference.
 * https://www.openpolicyagent.org/docs/latest/policy-reference/#encoding
 */
public final class EncodingBase64 {

    private EncodingBase64() {}

    private static final Expression DECODE = new
        @ExpressionArgument(JsonString.class)
        Expression() {
            @Override
            public JsonValue run(List<JsonValue> args, JsonObject input, JsonObject data) {
                try {
                    String encodedStr = ((JsonString) args.get(0)).getString();
                    byte[] rawBytes = Base64.getDecoder().decode(encodedStr);
                    return JsonResolver.nativeToJson(rawBytes);
                } catch (IllegalArgumentException e) {
                    throw new RegoProcessorException.BadReferenceException("String is not encoded");
                }
            }
        };

    public static final Map<String, Expression> EXPRESSIONS = Map.of(
        "base64.decode", DECODE
    );
}
