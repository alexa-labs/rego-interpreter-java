// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.lib;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonString;
import javax.json.JsonValue;

import com.amazon.antlr4.rego.interpreter.core.type.OperatorType;
import com.amazon.antlr4.rego.interpreter.core.type.RegoProcessorException;
import com.amazon.antlr4.rego.interpreter.core.type.SomeRule;

/**
 * Utility to apply infix operator to Json types.
 */
public final class InfixOperator {

    private InfixOperator() {}

    public static JsonValue applyInfixOperator(JsonValue lhs, OperatorType op, JsonValue rhs) {
        if (lhs instanceof SomeRule || rhs instanceof SomeRule) {
            return applyInfixOperatorOnSome(lhs, op, rhs);
        } else if (lhs instanceof JsonArray && rhs instanceof JsonArray) {
            return applyInfixOperator((JsonArray) lhs, op, (JsonArray) rhs);
        } else if (op == OperatorType.SET_AND || op == OperatorType.SET_OR) {
            throw new RegoProcessorException.UncheckedBadPolicyException("Set operator not valid for other types");
        } else if (lhs instanceof JsonString && rhs instanceof JsonString) {
            return applyInfixOperator(((JsonString) lhs).getString(), op, ((JsonString) rhs).getString());
        } else if (lhs instanceof JsonNumber && rhs instanceof JsonNumber) {
            return applyInfixOperator(((JsonNumber) lhs).bigDecimalValue(), op, ((JsonNumber) rhs).bigDecimalValue());
        }
        return applyInfixOperator(((JsonValue) lhs).toString(), op, ((JsonValue) rhs).toString());
    }

    private static JsonValue applyInfixOperatorOnSome(JsonValue lhs, OperatorType op, JsonValue rhs) {
        if (lhs instanceof SomeRule && rhs instanceof SomeRule) {
            throw new RegoProcessorException.UncheckedBadPolicyException(
                String.format("Variables are unsafe: %s, %s",
                    ((SomeRule) lhs).key, ((SomeRule) rhs).key));
        } else if (op != OperatorType.UNIFICATION) {
            throw new RegoProcessorException.UncheckedBadPolicyException(
                String.format("Operator %s not valid for null argument", op.getCode())
            );
        } else {
            unifySomeValue(lhs, rhs);
            return JsonValue.TRUE;
        }
    }

    private static void unifySomeValue(JsonValue lhs, JsonValue rhs) {
        if (lhs instanceof SomeRule) {
            ((SomeRule) lhs).setLastValue(rhs);
        } else {
            ((SomeRule) rhs).setLastValue(lhs);
        }
    }

    private static JsonValue applyInfixOperator(JsonArray lhs, OperatorType op, JsonArray rhs) {
        Set<JsonValue> lSet = new HashSet<>(lhs);
        Set<JsonValue> rSet = new HashSet<>(rhs);
        if (op == OperatorType.EQUAL) {
            if (lSet.equals(rSet)) {
                return JsonValue.TRUE;
            }
            return JsonValue.FALSE;
        } else if (op == OperatorType.UNIFICATION) {
            return unify(lhs, rhs);
        }
        return JsonResolver.nativeToJson(applyNativeOperator(lSet, op, rSet));
    }

    private static JsonValue unify(JsonArray lhs, JsonArray rhs) {
        if (lhs.size() != rhs.size()) {
            return JsonValue.FALSE;
        }
        for (int i = 0; i < lhs.size(); i++) {
            if (applyInfixOperator(lhs.get(i), OperatorType.UNIFICATION, rhs.get(i)) == JsonValue.FALSE) {
                return JsonValue.FALSE;
            }
        }
        return JsonValue.TRUE;
    }

    private static Set<JsonValue> applyNativeOperator(Set<JsonValue> lSet, OperatorType op, Set<JsonValue> rSet) {
        Set<JsonValue> dSet = new HashSet<>(lSet);
        switch (op) {
            case SET_AND:
                dSet.retainAll(rSet);
                break;
            case SET_OR:
                dSet.addAll(rSet);
                break;
            default:
                dSet.removeAll(rSet); // "-"
        }
        return dSet;
    }

    private static JsonValue applyInfixOperator(BigDecimal lhs, OperatorType op, BigDecimal rhs) {
        return JsonResolver.nativeToJson(applyNativeOperator(lhs, op, rhs));
    }

    private static Object applyNativeOperator(BigDecimal lhs, OperatorType op, BigDecimal rhs) {
        switch (op) {
            case UNIFICATION:
            case EQUAL: return lhs.equals(rhs);
            case NOT_EQUAL: return !lhs.equals(rhs);
            case GREATER_THAN_EQ: return lhs.compareTo(rhs) >= 0;
            case LESS_THAN_EQ: return lhs.compareTo(rhs) <= 0;
            case LESS_THAN: return lhs.compareTo(rhs) < 0;
            case GREATER_THAN: return lhs.compareTo(rhs) > 0;
            case ADD:  return lhs.add(rhs);
            case SUBTRACT: return lhs.subtract(rhs);
            case MULTIPLY: return lhs.multiply(rhs);
            case MODULO: return intModulo(lhs, rhs);
            default: return lhs.divide(rhs);
        }
    }

    private static BigDecimal intModulo(BigDecimal lhs, BigDecimal rhs) {
        try {
            lhs.toBigIntegerExact();
            rhs.toBigIntegerExact();
        } catch (ArithmeticException e) {
            throw new RegoProcessorException.BadReferenceException("Wanted integer, got float", e);
        }
        return lhs.remainder(rhs);
    }

    private static JsonValue applyInfixOperator(String lhs, OperatorType op, String rhs) {
        return JsonResolver.nativeToJson(applyNativeOperator(lhs, op, rhs));
    }

    private static Object applyNativeOperator(String lhs, OperatorType op, String rhs) {
        switch (op) {
            case UNIFICATION:
            case EQUAL: return lhs.equals(rhs);
            case NOT_EQUAL: return !lhs.equals(rhs);
            default:   return lhs + rhs;
        }
    }
}
