// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.lib.expr;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonValue;

import com.amazon.antlr4.rego.interpreter.core.lib.ExprLibrary;
import com.amazon.antlr4.rego.interpreter.core.lib.JsonResolver;
import com.amazon.antlr4.rego.interpreter.type.Expression;
import com.amazon.antlr4.rego.interpreter.type.ExpressionArgument;

/**
 * Number functions from Rego language reference.
 * https://www.openpolicyagent.org/docs/latest/policy-reference/#numbers
 */
public final class Numbers {

    private Numbers() {}

    private static JsonValue setScale(List<JsonValue> args, RoundingMode roundingMode) {
        ExprLibrary.matchArgumentTypes(List.of(JsonNumber.class), args);
        BigDecimal number = ((JsonNumber) args.get(0)).bigDecimalValue().setScale(0, roundingMode);
        return JsonResolver.nativeToJson(number.toBigInteger());
    }

    private static final Expression NUMOP_ROUND = (args, i, d) -> setScale(args, RoundingMode.HALF_UP);
    private static final Expression NUMOP_CEIL = (args, i, d) -> setScale(args, RoundingMode.CEILING);
    private static final Expression NUMOP_FLOOR = (args, i, d) -> setScale(args, RoundingMode.FLOOR);

    private static final Expression NUMOP_ABS = new
        @ExpressionArgument(JsonNumber.class)
        Expression() {
            @Override
            public JsonValue run(List<JsonValue> args, JsonObject input, JsonObject data) {
                BigDecimal number = ((JsonNumber) args.get(0)).bigDecimalValue();
                if (number.compareTo(BigDecimal.ZERO) < 0) {
                    number = number.multiply(BigDecimal.valueOf(-1));
                }
                return JsonResolver.nativeToJson(number);
        }
    };

    private static final Expression NUMOP_RANGE = new
        @ExpressionArgument(JsonNumber.class)
        @ExpressionArgument(JsonNumber.class)
        Expression() {
            @Override
            public JsonValue run(List<JsonValue> args, JsonObject input, JsonObject data) {
                BigDecimal n0 = ((JsonNumber) args.get(0)).bigDecimalValue();
                BigDecimal n1 = ((JsonNumber) args.get(1)).bigDecimalValue();
                BigInteger start, end, iterate;
                if (n0.compareTo(n1) <= 0) {
                    start = n0.setScale(0, RoundingMode.CEILING).toBigInteger();
                    end = n1.setScale(0, RoundingMode.FLOOR).toBigInteger();
                    iterate = BigInteger.ONE;
                } else {
                    start = n0.setScale(0, RoundingMode.FLOOR).toBigInteger();
                    end = n1.setScale(0, RoundingMode.CEILING).toBigInteger();
                    iterate = BigInteger.valueOf(-1);
                }
                return JsonResolver.nativeToJson(
                    Stream.iterate(start, i -> i.compareTo(end) != iterate.intValue(), i -> i.add(iterate))
                );
            }
        };

    public static final Map<String, Expression> EXPRESSIONS = Map.of(
        "round", NUMOP_ROUND,
        "ceil", NUMOP_CEIL,
        "floor", NUMOP_FLOOR,
        "abs", NUMOP_ABS,
        "numbers.range", NUMOP_RANGE
    );
}
