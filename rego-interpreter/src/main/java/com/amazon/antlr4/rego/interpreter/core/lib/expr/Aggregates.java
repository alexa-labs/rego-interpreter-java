// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.lib.expr;

import java.math.BigDecimal;
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
 * Aggregate functions from Rego language reference.
 * https://www.openpolicyagent.org/docs/latest/policy-reference/#aggregates
 */
public final class Aggregates {

    private Aggregates() {}

    private static final Expression COUNT = new
        @ExpressionArgument(JsonValue.class)
        Expression() {
            @Override
            public JsonValue run(List<JsonValue> args, JsonObject input, JsonObject data) {
                JsonValue arg0 = args.get(0);
                if (arg0 instanceof JsonArray) {
                    return JsonResolver.nativeToJson(new BigDecimal(((JsonArray) arg0).size()));
                } else if (arg0 instanceof JsonString) {
                    return JsonResolver.nativeToJson(new BigDecimal(((JsonString) arg0).getString().length()));
                } else if (arg0 instanceof JsonObject) {
                    return JsonResolver.nativeToJson(new BigDecimal(((JsonObject) arg0).size()));
                }
                throw new RegoProcessorException.UncheckedBadPolicyException("Can only count lists or strings");
            }
        };

    private interface Worker {
        BigDecimal compound(BigDecimal src, BigDecimal dst);
    }

    private static Worker sumWorker = (src, dst) -> src.add(dst);
    private static Worker productWorker = (src, dst) -> src.multiply(dst);
    private static Worker maxWorker = (src, dst) -> src.compareTo(dst) > 0 ? src : dst;
    private static Worker minWorker = (src, dst) -> src.compareTo(dst) < 0 ? src : dst;

    private static JsonValue iterate(List<JsonValue> args, Worker worker, BigDecimal init, boolean emptyOk) {
        if (args.size() != 1 || !(args.get(0) instanceof JsonArray)) {
            throw new RegoProcessorException.UncheckedBadPolicyException("Expect single parameter of type list");
        }
        JsonArray arg0 = (JsonArray) args.get(0);
        if (arg0.isEmpty() && !emptyOk) {
            throw new RegoProcessorException.BadReferenceException("Empty collection not acceptable");
        }
        BigDecimal compound = init;
        for (JsonValue o: arg0) {
            if (o instanceof JsonNumber) {
                if (compound == null) {
                    compound = ((JsonNumber) o).bigDecimalValue();
                }
                compound = worker.compound(((JsonNumber) o).bigDecimalValue(), compound);
            } else {
                throw new RegoProcessorException.BadReferenceException("Number expected: " + o);
            }
        }
        return JsonResolver.nativeToJson(compound);
    }

    private static final Expression SUM = (args, i, d) -> iterate(args, sumWorker, new BigDecimal(0), true);
    private static final Expression PRODUCT = (args, i, d) -> iterate(args, productWorker, new BigDecimal(1), true);
    private static final Expression MAX = (args, i, d) -> iterate(args, maxWorker, null, false);
    private static final Expression MIN = (args, i, d) -> iterate(args, minWorker, null, false);

    private interface Booler {
        boolean compound(boolean src, boolean dst);
    }

    private static Booler allBooler = (src, dst) -> src && dst;
    private static Booler anyBooler = (src, dst) -> src || dst;

    private static JsonValue boolExpr(List<JsonValue> args, Booler booler, boolean init) {
        if (args.size() != 1 || !(args.get(0) instanceof JsonArray)) {
            throw new RegoProcessorException.UncheckedBadPolicyException("Expect single parameter of type list");
        }
        JsonArray arg0 = (JsonArray) args.get(0);
        boolean result = init;
        for (JsonValue o: arg0) {
            result = booler.compound(o == JsonValue.TRUE, result);
        }
        return result ? JsonValue.TRUE : JsonValue.FALSE;
    }

    private static final Expression ALL = (args, input, data) -> boolExpr(args, allBooler, true);
    private static final Expression ANY = (args, input, data) -> boolExpr(args, anyBooler, false);

    public static final Map<String, Expression> EXPRESSIONS = Map.of(
        "all", ALL,
        "any", ANY,
        "count", COUNT,
        "max", MAX,
        "min", MIN,
        "product", PRODUCT,
        "sum", SUM
    );
}
